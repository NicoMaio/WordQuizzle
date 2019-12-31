import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;

public class Worker implements Runnable {

    private final TreeMap<String, Utente> registeredList;
    public static String dizionario = "Dizionario.json";
    private static int K_BOUND = 5;
    private TreeMap<String, SelectionKey> usersList;
    private ServerService.Con con;
    private SelectionKey key;
    private Selector selector;
    private SelectionKey sfidante;
    private SelectionKey sfidato;
    private int sfidat;
    private int sfidant;
    private boolean endingSfida;


    private static int BUF_DIM = 1024;
    public Worker(ServerService.Con con,TreeMap<String, Utente> registeredList,
                  TreeMap<String, SelectionKey> usersList,SelectionKey key) {
        this.con  = con;
        this.registeredList = registeredList;
        this.usersList = usersList;
        this.key = key;

        sfidat = 0;
        sfidant = 0;
        endingSfida= false;
    }

    @Override
    public void run() {

        con.req.flip();
        String receive = StandardCharsets.UTF_8.decode(con.req).toString();
        System.out.println("ecco stringa ricevuta: "+receive);
        String[] elenco = receive.split("/");
        SocketChannel client =(SocketChannel) key.channel();


        if(elenco.length >0){
            switch (elenco[0]){
                case "login" : // typeOp 1
                    String username = elenco[1];
                    String password = elenco[2];

                    Utente utente= null;
                    synchronized (registeredList) {
                        if (registeredList.containsKey(username)) {
                            utente = registeredList.get(username);
                        }
                    }
                    String response="";
                    /*
                    *   response = 0 : login già effettuato;
                    *   response = 1 : login ok;
                    *   response = -1 : password sbagliata;
                    *   responde = -2 : utente non registrato;
                    * */
                    if(utente!=null){
                        if(password.equals(utente.getPassword())){
                            boolean ok=false;
                            synchronized (usersList){
                                if(usersList.containsKey(utente.getUsername())){
                                    ok = true;
                                } else {
                                    usersList.put(username,key);
                                }
                            }

                            if(ok){
                                // login già effettuato

                                response += "0";

                            } else {
                                // login ok
                                response+= "1";
                            }
                        } else {
                            // password sbagliata
                            response+= "-1";
                        }
                    } else {
                        // utente non è registrato
                        response+= "-2";
                    }

                    // salvo risposta per client in con.resp
                    con.typeOp = 1;
                    con.resp = ByteBuffer.wrap(response.getBytes());

                    System.out.println(new String(con.resp.array()));
                    //key.attach(con);
                    //key.interestOps(SelectionKey.OP_WRITE);

                    //key.attach(con);
                    //ServerService.tryWrite(key);

                    try {
                        client.write(con.resp);
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                break;
                case "logout": // typeOp 2
                    String uname = elenco[1];

                    boolean ok = false;
                    synchronized (usersList){
                        if(usersList.containsKey(uname)){
                            usersList.remove(uname);
                            ok = true;
                        }
                    }

                    String resp ="";
                    /*
                    *   resp = 1 : logout ok;
                    *   resp = -1 : utente non era online.
                    *
                    * */
                    if(ok){
                        // logout ok = 1
                        resp += "1";
                    } else {
                        resp += "-1";
                    }

                    con.typeOp = 2;
                    con.resp = ByteBuffer.wrap(resp.getBytes());

                    System.out.println(new String(con.resp.array()));
                    //key.attach(con);
                    //key.interestOps(SelectionKey.OP_WRITE);



                    try {
                        client.write(con.resp);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "lista_amici":
                    String user = elenco[1];

                    Utente u = null;
                    synchronized (registeredList){
                        u=registeredList.get(user);
                    }

                    if(u!=null){
                        Vector<String> friends = u.getFriends();

                        JSONArray array = new JSONArray();

                        Iterator<String> iterator = friends.iterator();
                        while(iterator.hasNext()){
                            JSONObject obj = new JSONObject();
                            obj.put("username",iterator.next());
                            array.add(obj);
                        }

                        String result = array.toJSONString();


                        con.resp = ByteBuffer.wrap(result.getBytes());

                        try {
                            client.write(con.resp);
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                        con.clearAll();
                        key.attach(con);
                    }
                break;
                case "aggiungi_amico":
                    String us = elenco[1];
                    String nfriend = elenco[2];

                    int isok = 0;
                    synchronized (registeredList){

                        if(registeredList.get(nfriend)!=null){

                            Vector<String> friends = registeredList.get(us).getFriends();
                            if(friends.contains(nfriend)){
                                isok= -2;
                            } else {

                                registeredList.get(us).setFriend(nfriend);
                                registeredList.get(nfriend).setFriend(us);
                                isok = 1;

                            }
                        } else {
                            isok= -1;
                        }
                    }

                    String risp ="";
                    /*
                    *   risp = 1: aggiungi amico ok;
                    *   risp = -1: amico non esistente;
                    *   risp = -2: amico precedentemente aggiunto.
                    *
                    * */

                    risp += isok;

                    con.resp = ByteBuffer.wrap(risp.getBytes());

                    try {
                        client.write(con.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    ServerService.saveUsersStats(registeredList);
                break;
                case "mostra_punteggio":
                    String ute = elenco[1];

                    long punteggio = 0;

                    synchronized (registeredList){
                        punteggio= registeredList.get(ute).getPoint();
                    }

                    String risposta =""+ punteggio;

                    con.resp = ByteBuffer.wrap(risposta.getBytes());

                    try {
                        client.write(con.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "mostra_classifica":
                    String ut = elenco[1];
                    Map<String,Long> classifica = new TreeMap<String,Long>();

                    synchronized (registeredList){
                        classifica.put(ut,registeredList.get(ut).getPoint());

                        Vector<String> friends = registeredList.get(ut).getFriends();

                        Iterator<String> iterator = friends.iterator();

                        while(iterator.hasNext()){
                            String friend = iterator.next();
                            classifica.put(friend,registeredList.get(friend).getPoint());
                        }
                    }

                    SortedSet<Map.Entry<String,Long>> result = entriesSortedByValues(classifica);
                    Iterator<Map.Entry<String,Long>>iterator= result.iterator();
                    JSONArray array = new JSONArray();

                    while(iterator.hasNext()){
                        JSONObject obj = new JSONObject();
                        Map.Entry<String,Long> next = iterator.next();
                        obj.put("username",next.getKey());
                        obj.put("points",next.getValue());
                        array.add(obj);
                    }

                    String rispo = array.toJSONString();

                    con.resp = ByteBuffer.wrap(rispo.getBytes());
                    try {
                        client.write(con.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "sfida":
                    String uo = elenco[1];
                    String friend = elenco[2];
                    String respo = "";

                    int isafriend = 1;
                    synchronized (registeredList){
                        if(!registeredList.get(uo).getFriends().contains(friend)){
                            isafriend = 0;
                        }
                    }

                    if(isafriend == 0){
                        // invio messaggio d'errore e termino

                        respo += "Utente "+friend+" non è un tuo amico.";

                        con.resp = ByteBuffer.wrap(respo.getBytes());
                        try {
                            client.write(con.resp);
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    } else {
                        // invio sfida ad amico solo se è online.
                        // se non è online invio messaggio d'errore.
                        int error = 1;

                        SelectionKey amico = null;
                        synchronized (usersList){
                            if(!usersList.containsKey(friend)){
                                error = 0;
                            } else {
                                amico = usersList.get(friend);
                            }
                        }

                        if(error == 0) {

                            // invio messaggio d'errore amico sfidato non è online
                            respo += "Amico "+friend+" non è online.";

                            con.resp = ByteBuffer.wrap(respo.getBytes());
                            try {
                                client.write(con.resp);
                            } catch (IOException e){
                                e.printStackTrace();
                            }
                        } else {

                            ServerService.dontRead(key,amico);

                            Random rand = new Random();

                            IntStream stream = rand.ints(13200,15200);

                            try {

                                // inizializzo selector e datagramchannel
                                selector = Selector.open();
                                DatagramChannel channel = DatagramChannel.open();

                                Iterator<Integer> iterator1 = stream.iterator();
                                int port = iterator1.next();

                                InetSocketAddress isa = new InetSocketAddress(port);

                                try {
                                    channel.socket().bind(isa);

                                } catch (AlreadyBoundException s){

                                    port = useAnotherPort(iterator1, channel);
                                }
                                channel.configureBlocking(false);

                                ServerService.callBack(friend,port);
                                respo += "Scrivi qui:"+port;

                                con.resp = ByteBuffer.wrap(respo.getBytes());
                                try {
                                    client.write(con.resp);
                                } catch (IOException e){
                                    e.printStackTrace();
                                }

                                // configuro non bloccante il DatagramChannel

                                SelectionKey clientkey;

                                try {
                                    clientkey = channel.register(selector,SelectionKey.OP_READ);
                                    clientkey.attach(new Auxiliar(uo,friend));
                                } catch (ClosedChannelException cce){
                                    cce.printStackTrace();
                                }
                                while(!endingSfida || (sfidant ==-1 && sfidat == -1)){
                                    selector.select();
                                    Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                                    while(selectedKeys.hasNext()){
                                        SelectionKey key = (SelectionKey) selectedKeys.next();
                                        selectedKeys.remove();

                                        if(!key.isValid()) continue;
                                        if(key.isReadable()){
                                            readUDPreq(key);
                                        }
                                        else if (key.isWritable()) {
                                            sendUDPreq(key);
                                        }

                                    }

                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                                ServerService.saveUsersStats(registeredList);

                                return;
                            }
                            // invio richiesta ad amico
                            // aspetto tempo T1 = 10 sec
                            // se accetta creo lista parole
                            // se non torna risposta entro 10 sec o rifiuta
                            // invio messaggio d'errore.

                            // se accetta dopo aver creato la lista imposto T2 = 60 sec
                            // N = 30
                            // K = random % 15
                            // X: punteggio per risposta ok = +2
                            // Y: punteggio per risposta not ok = -1

                            // aggiorno punteggio chiudo selector e tutto e riaggiungo le chiavi
                            // dei due sfidanti al selector TCP
                            if(!endingSfida){
                                // calcolo risultati e invio risposto ai due giocatori.
                                Auxiliar tempSfidante = (Auxiliar)sfidante.attachment();
                                int puntsfidant = tempSfidante.getPunteggio();
                                int paroleOkSfidant = tempSfidante.getParoleOk();
                                int paroleNotOkSfidant = tempSfidante.getParoleNotOk();
                                int paroleNotTraSfidant = tempSfidante.getParoleNotTra();

                                Auxiliar tempSfidato = (Auxiliar)sfidato.attachment();
                                int puntsfidat = tempSfidato.getPunteggio();
                                int paroleOkSfidat = tempSfidato.getParoleOk();
                                int paroleNotOkSfidat = tempSfidato.getParoleNotOk();
                                int paroleNotTraSfidat = tempSfidato.getParoleNotTra();

                                String risp1,risp2;
                                if(puntsfidat < puntsfidant){
                                    int finalpunt = puntsfidant+3;
                                    sendToWinner(tempSfidante, puntsfidant, paroleOkSfidant, paroleNotOkSfidant, paroleNotTraSfidant, puntsfidat, finalpunt);

                                    synchronized (registeredList){
                                        registeredList.get(tempSfidante.sfidante).setPoint(finalpunt);
                                        registeredList.get(tempSfidato.sfidato).setPoint(puntsfidat);
                                    }

                                    sendToLoser(puntsfidant, tempSfidato, puntsfidat, paroleOkSfidat, paroleNotOkSfidat, paroleNotTraSfidat);

                                } else if(puntsfidant == puntsfidat){
                                    sendRisultato(tempSfidante, puntsfidant, paroleOkSfidant, paroleNotOkSfidant, paroleNotTraSfidant, puntsfidat);

                                    synchronized (registeredList){
                                        registeredList.get(tempSfidante.sfidante).setPoint(puntsfidant);
                                        registeredList.get(tempSfidato.sfidato).setPoint(puntsfidat);
                                    }

                                    sendRisultato(tempSfidato, puntsfidat, paroleOkSfidat, paroleNotOkSfidat, paroleNotTraSfidat, puntsfidant);
                                } else {
                                    int finalpunt = puntsfidat+3;
                                    sendToLoser(puntsfidat, tempSfidante, puntsfidant, paroleOkSfidant, paroleNotOkSfidant, paroleNotTraSfidant);

                                    synchronized (registeredList){
                                        registeredList.get(tempSfidante.sfidante).setPoint(puntsfidant);
                                        registeredList.get(tempSfidato.sfidato).setPoint(finalpunt);
                                    }

                                    sendToWinner(tempSfidato, puntsfidat, paroleOkSfidat, paroleNotOkSfidat, paroleNotTraSfidat, puntsfidant, finalpunt);
                                }
                            }
                            try {
                                selector.close();
                                key.cancel();
                                sfidante.cancel();
                                sfidato.cancel();
                            } catch (IOException e){
                                e.printStackTrace();
                            }
                            ServerService.abilityRead(key,amico);
                            ServerService.saveUsersStats(registeredList);
                        }
                    }

                break;
                default:
                break;
            }
        }
        return;
    }

    private void sendToLoser(int puntsfidant, Auxiliar tempSfidato, int puntsfidat, int paroleOkSfidat, int paroleNotOkSfidat, int paroleNotTraSfidat) {
        String risp2;
        risp2 = "Hai tradotto correttamente "+paroleOkSfidat+", ne hai sbagliate "+paroleNotOkSfidat+" e non hai risposto a "+paroleNotTraSfidat+"."
                +"\nHai totalizzato "+puntsfidat+" punti."+
                "\nIl tuo avversario ha totatlizzato "+puntsfidant+" punti."+
                "\nHai perso la sfida...";
        tempSfidato.resp = ByteBuffer.wrap(risp2.getBytes());
        DatagramChannel chan2 = (DatagramChannel) key.channel();
        try {
            chan2.send(tempSfidato.resp, tempSfidato.sa);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendToWinner(Auxiliar tempSfidante, int puntsfidant, int paroleOkSfidant, int paroleNotOkSfidant, int paroleNotTraSfidant, int puntsfidat, int finalpunt) {
        String risp1;
        risp1 = "Hai tradotto correttamente "+paroleOkSfidant+", ne hai sbagliate "+paroleNotOkSfidant+" e non hai risposto a "+paroleNotTraSfidant+"."
                +"\nHai totalizzato "+puntsfidant+" punti."+
                "\nIl tuo avversario ha totatlizzato "+puntsfidat+" punti."+
                "\nCongratulazioni, hai vinto! Hai guadagnato 3 punti extra, per un totale di "+finalpunt+" punti!";
        tempSfidante.resp = ByteBuffer.wrap(risp1.getBytes());
        DatagramChannel chan = (DatagramChannel) key.channel();
        try {
            chan.send(tempSfidante.resp, tempSfidante.sa);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendRisultato(Auxiliar tempSfidante, int puntsfidant, int paroleOkSfidant, int paroleNotOkSfidant, int paroleNotTraSfidant, int puntsfidat) {
        String risp1;
        risp1 = "Hai tradotto correttamente "+paroleOkSfidant+", ne hai sbagliate "+paroleNotOkSfidant+" e non hai risposto a "+paroleNotTraSfidant+"."
                +"\nHai totalizzato "+puntsfidant+" punti."+
                "\nIl tuo avversario ha totatlizzato "+puntsfidat+" punti."+
                "\nLa sfida è finita in parità";
        tempSfidante.resp = ByteBuffer.wrap(risp1.getBytes());
        DatagramChannel chan = (DatagramChannel) key.channel();
        try {
            chan.send(tempSfidante.resp, tempSfidante.sa);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    static <K,V extends Comparable<? super V>>
    SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e2.getValue().compareTo(e1.getValue());
                        return res != 0 ? res : 1;
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public class Auxiliar {

        ByteBuffer req;
        ByteBuffer resp;
        SocketAddress sa;
        Vector<String> wordsDatradurre;
        Vector<String> wordsTradotte;
        String sfidante;
        String sfidato;
        private int paroleOk;
        private int paroleNotOk;
        private int paroleNotTra;

        private int id;

        private int punteggio;
        /*
         *   id= 1 : sfidante;
         *   id= 2 : sfidato;
         *
         * */

        public Auxiliar(String sfidante,String sfidato) {
            req = ByteBuffer.allocate(BUF_DIM);
            this.sfidante = sfidante;
            this.sfidato = sfidato;
            punteggio = 0;
            paroleNotOk =0;
            paroleNotTra =0;
            paroleOk =0;
        }

        public void clearAll(){
            req.clear();
            resp.clear();
        }

        public void setWords(Vector<String> parole){
            wordsDatradurre = parole;
        }

        public void setWordsTradotte(Vector<String> parole){
            wordsTradotte = parole;
        }

        public String getWord(int index){
            return wordsDatradurre.get(index);
        }

        public boolean containsTrad(String word){
            return wordsTradotte.contains(word);
        }

        public void setId(int value){
            id = value;
        }

        public int getId(){
            return id;
        }

        public void setPunteggio(int value){
            punteggio+=value;
        }

        public int getPunteggio(){
            return punteggio;
        }

        public void setParoleOk(int value){
            paroleOk+=value;
        }

        public void setParoleNotOk(int value){
            paroleNotOk+=value;
        }

        public void setParoleNOtTra(int value){
            paroleNotTra+=value;
        }

        public int getParoleOk(){
            return paroleOk;
        }

        public int getParoleNotOk(){
            return paroleNotOk;
        }

        public int getParoleNotTra(){
            return paroleNotTra;
        }
    }

    private void readUDPreq(SelectionKey key) throws IOException {
        DatagramChannel chan = (DatagramChannel) key.channel();
        Auxiliar aux = (Auxiliar) key.attachment();
        aux.sa = chan.receive(con.req);
        // ricevo richiesta da client

        // devo ricevere richieste iniziali

        String req = StandardCharsets.UTF_8.decode(con.req).toString();
        String[] elenco = req.split("/");
        String send;
        switch (elenco[0]){
            case "sfidato":
                send = "Sei stato sfidato da "+aux.sfidante+" vuoi accettare?/10";
                aux.resp = ByteBuffer.wrap(send.getBytes());
                key.attach(aux);
                sfidato = key;

                key.interestOps(SelectionKey.OP_WRITE);
                break;
            case "sfidante":
                send = "In attesa dello sfidato: "+aux.sfidato;
                aux.resp = ByteBuffer.wrap(send.getBytes());
                key.attach(aux);
                sfidante = key;
                key.interestOps(SelectionKey.OP_WRITE);
                break;
            case "ok":
                // seleziono le parole da dizionario e le traduco per iniziare sfida
                // seleziono tempo per sfida e lo invio insieme con la prima parola,
                // lato client settaranno il loro thread Timeout.

                Random rand = new Random();
                int K = rand.nextInt(K_BOUND)+1;
                Path path = Paths.get(".");
                Path JsonNioPath = path.resolve(dizionario);
                Vector<String> words = null;
                if(Files.exists(JsonNioPath)){
                    String elements;
                    try{

                        elements = MainClassServer.readJson(dizionario);
                        words = takeWordsFromJson(K,elements);

                    } catch (IOException e){
                        e.printStackTrace();
                    }

                }

                Auxiliar aux1 = (Auxiliar) sfidante.attachment();
                Auxiliar aux2 = (Auxiliar) sfidato.attachment();

                aux1.setId(1);
                aux2.setId(2);
                aux1.setWords(words);
                aux2.setWords(words);

                Vector<String> traduzioni = translateWords(words);

                aux1.setWordsTradotte(traduzioni);
                aux2.setWordsTradotte(traduzioni);

                // invio tempo della sfida e prima parola ad entrambi

                String resp = "60/K/"+words.get(0);

                aux1.resp = ByteBuffer.wrap(resp.getBytes());
                aux2.resp = ByteBuffer.wrap(resp.getBytes());

                sfidante.attach(aux1);
                sfidato.attach(aux2);

                sfidat = 1;
                sfidant = 1;
                sfidato.interestOps(SelectionKey.OP_WRITE);
                sfidante.interestOps(SelectionKey.OP_WRITE);

                break;
            case "not ok":

            case "tempo scaduto per accettare":
                // invio risposta allo sfidante e chiudo tutto

                String risp= "sfida non accettata";
                Auxiliar auxs = (Auxiliar) sfidante.attachment();
                auxs.resp = ByteBuffer.wrap(risp.getBytes());
                sfidante.attach(auxs);
                sfidante.interestOps(SelectionKey.OP_WRITE);
                break;

            case "parola":
                // leggo parola tradotta aggiorno punteggio e mando parola successiva
                // se ancora non sono finite altrimenti aspetto altro utente che finisca
                // per mandare i risultati della sfida.
                // parola/1/K/parolaTradotta

                int actual = Integer.parseInt(elenco[1]);
                int total = Integer.parseInt(elenco[2]);

                String word = elenco[3];

                // calcolo risultato.
                Auxiliar temp = (Auxiliar) key.attachment();
                if(word.equals(" ")){
                    temp.setPunteggio(0);
                    temp.setParoleNOtTra(1);
                } else if(temp.containsTrad(word)){
                    temp.setPunteggio(2);
                    temp.setParoleOk(1);
                } else {
                    temp.setPunteggio(-1);
                    temp.setParoleNotOk(1);
                }

                if(actual>=total){
                    int id = temp.getId();
                    endOne(key, temp, id);
                } else {
                    String newWord = temp.getWord(actual+1);
                    String response = "parola/"+actual+"/"+total+newWord;
                    temp.resp = ByteBuffer.wrap(response.getBytes());
                    key.attach(temp);
                    key.interestOps(SelectionKey.OP_WRITE);
                }
                break;
            case "tempo scaduto per rispondere":
                Auxiliar temp1 = (Auxiliar) key.attachment();

                int id1 = temp1.getId();
                endOne(key, temp1, id1);

                break;
        }

    }

    private void endOne(SelectionKey key, Auxiliar temp1, int id1) {
        switch (id1){
            case 1:
                sfidant = -1;
                sfidante = key;
                /*synchronized (registeredList) {
                    registeredList.get(temp1.sfidante).setPoint(temp1.getPunteggio());
                }*/
                break;
            case 2:
                sfidat = -1;
                sfidato = key;
                /*synchronized (registeredList) {
                    registeredList.get(temp1.sfidato).setPoint(temp1.getPunteggio());
                }*/
                break;
        }
    }

    private Vector<String> translateWords(Vector<String> words){
        Vector<String> tradotte = new Vector<>();

        Iterator<String> iterator = words.iterator();
        while(iterator.hasNext()) {
            try {
                URL url1 = new URL("https://api.mymemory.translated.net/get?q=" + iterator.next() + "&langpair=en|it");


                try (BufferedReader in = new BufferedReader(new InputStreamReader(url1.openStream()))) {
                    StringBuilder inputLine= new StringBuilder();
                    String reader;
                    // Read the "gpl.txt" text file from its URL representation
                    while ((reader = in.readLine()) != null) {
                        inputLine.append(reader);
                    }

                    JSONObject jsonObject;
                    JSONParser parser = new JSONParser();

                    try {
                        jsonObject = (JSONObject) parser.parse(inputLine.toString());
                        JSONObject result = (JSONObject) jsonObject.get("responseData");

                        //String stampa = (String) result.get("translatedText");
                        //System.out.println(stampa);

                        JSONArray array = (JSONArray) jsonObject.get("matches");

                        Iterator<JSONObject> iterator2 = array.iterator();
                        while (iterator2.hasNext()){
                            String stampa1 = (String)iterator2.next().get("translation");
                            tradotte.add(stampa1);
                        }
                    } catch (ParseException e){
                        e.printStackTrace();
                    }

                } catch (IOException ioe) {
                    ioe.printStackTrace(System.err);
                }
            } catch (MalformedURLException mue) {
                mue.printStackTrace(System.err);
            }
        }


        return tradotte;
    }

    private void sendUDPreq(SelectionKey key){
        DatagramChannel chan = (DatagramChannel) key.channel();
        Auxiliar aux = (Auxiliar) key.attachment();

        String risp = StandardCharsets.UTF_8.decode(aux.resp).toString();
        if(!risp.contains("parola")){
            try {
                chan.send(aux.resp, aux.sa);
            } catch (IOException e){
                e.printStackTrace();
            }
            key.interestOps(SelectionKey.OP_READ);
        } else if(risp.contains("sfida non accettata")){

            try {
                chan.send(aux.resp, aux.sa);
            } catch (IOException e){
                e.printStackTrace();
            }

            key.cancel();
            sfidante.cancel();
            sfidato.cancel();
            endingSfida = true;
        } else {
            try {
                chan.send(aux.resp, aux.sa);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    private int useAnotherPort(Iterator<Integer> iterator,DatagramChannel channel){
        boolean findIt = false;
        int nextPort =13201;
        while(!findIt){
            nextPort = iterator.next();
            try {
                InetSocketAddress isa = new InetSocketAddress(nextPort);

                channel.socket().bind(isa);

                findIt = true;
            } catch (Exception a){ }
        }
        return nextPort;
    }

    private Vector<String> takeWordsFromJson(int k,String json){
        Vector<String> result = new Vector<>();
        Vector<String> dictionary = new Vector<>();
        JSONArray jsonArray;
        JSONParser parser = new JSONParser();

        // server per eliminare duplicati
        List<String> listaInput = new ArrayList<>();

        try{
            jsonArray = (JSONArray) parser.parse(json);
            // ottengo array con tutti gli utenti

            Iterator<JSONObject> iterator = jsonArray.iterator();
            while(iterator.hasNext()){
                JSONObject obj = iterator.next();
                String word =(String)obj.get("username");
                dictionary.add(word);

            }

            while(listaInput.size()<k){
                Random rand = new Random();
                int index = rand.nextInt(dictionary.size());
                result.add(dictionary.get(index));
                listaInput = new ArrayList<String>(new LinkedHashSet<String>(result));

            }

            result.removeAllElements();

            result.addAll(listaInput);

        }catch (ParseException e){
            e.printStackTrace();
        }
        return result;

    }
}
