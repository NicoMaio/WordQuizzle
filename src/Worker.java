/**
 * @author Nicolò Maio
 *
 * Task eseguito dal thread lanciato dal threadPoolExecutor che porta a termine le richieste fatte dai client e invia risposta.
 * */

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

    private final  TreeMap<String, Utente> registeredList;
    // TreeMap contenente tutte le info sugli utenti.
    
    public static String dizionario = "Dizionario.json";
    // Nome del file in cui sono contenute le parole da usare per le sfide.
    
    private final TreeMap<String, SelectionKey> usersList;
    // TreeMap contenente Nome e SelectionKey degli utenti online.
    
    private final Vector<String> gamers;
    // Vector contenente gli username degli utenti impegnati in sfide. 
    
    private SelectionKey key;
    // SelectionKey dell'utente che ha richiesto l'operazione. 
    
    private Selector selector;
    // Selector usato per gestire le richieste UDP.
    
    /* --- Variabili SelectionKey ausiliarie --- */
    private static SelectionKey sfidante;
    private static SelectionKey sfidante1;
    private static SelectionKey sfidato1;
    /* ----------------------------------------- */

    private static int sfidat;
    private static int sfidant;
    // variabili intere usate per capire quando sfida non è stata accettata dunque far terminare il ciclo del selector UDP.
    
    private boolean startingSfida;
    // booleano usato per capire se far iniziare la sfida, ovvero se è stata accettata dallo sfidato.

    static SocketChannel client;
    // SocketChannel del client che ha inviato richiesta.

    private Counters counters;
    // Counters usata per contenere i contatori degli sfidanti.

    public Worker(TreeMap<String, Utente> registeredList,
                  TreeMap<String, SelectionKey> usersList,SelectionKey key,
                  Vector<String> gamers,Counters counters) {
        this.usersList = usersList;
        this.key = key;
        this.gamers = gamers;
        this.counters = counters;
        this.registeredList = registeredList;
        sfidat = 5;
        sfidant = 5;
        startingSfida= false;
    }

    @Override
    public void run() {

        // leggo richiesta appena ricevuto e switcho sulla prima parola.
        // N.B. le richieste vengono composte nel seguente modo es. login/Michele/Superman, dove la prima parola
        // indica il tipo di richiesta e le successive il nome dell'utente che l'ha richiesta e ulteriori info.
        String receive = null;
        try {
            Auxiliar con = (Auxiliar) key.attachment();
            con.req.flip();
            receive = StandardCharsets.UTF_8.decode(con.req).toString();
            System.out.println("ecco stringa ricevuta: " + receive);
            client = (SocketChannel) key.channel();

        } catch (ClassCastException e){
            e.printStackTrace();
        }
        assert receive != null;
        String[] elenco = receive.split("/");

        if(elenco.length >0){

            switch (elenco[0]){
                case "login" :
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

                    // invio risposta al client.
                    Auxiliar con = (Auxiliar)key.attachment();
                    con.resp = ByteBuffer.wrap(response.getBytes());

                    try {
                        client.write(con.resp);
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                break;
                case "logout":
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

                    Auxiliar con9 = (Auxiliar)key.attachment();

                    con9.resp = ByteBuffer.wrap(resp.getBytes());

                    synchronized (gamers) {
                        gamers.remove(uname);
                    }

                    // invio risposta al client.
                    try {
                        client.write(con9.resp);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "lista_amici":

                    String user = elenco[1];
                    Utente u;

                    synchronized (registeredList){
                        u=registeredList.get(user);
                    }

                    // compongo stringa JSON e la invio al client.
                    if(u!=null){
                        Vector<String> friends = u.getFriends();

                        JSONArray array = new JSONArray();

                        for (String friend : friends) {
                            JSONObject obj = new JSONObject();
                            obj.put("username", friend);
                            array.add(obj);
                        }

                        String result = array.toJSONString();
                        Auxiliar con8 = (Auxiliar)key.attachment();

                        con8.resp = ByteBuffer.wrap(result.getBytes());

                        try {
                            client.write(con8.resp);
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                        con8.clearAll();
                        key.attach(con8);
                    }
                break;
                case "aggiungi_amico":
                    String us = elenco[1];
                    String nfriend = elenco[2];

                    int isok;

                    synchronized (registeredList){

                        if(registeredList.get(nfriend)!=null){

                            Vector<String> friends = registeredList.get(us).getFriends();
                            if(friends.contains(nfriend)){
                                isok= -2;
                            } else {

                                // aggiungo associazione amico sia da utente che la richiesta verso utente indicato,
                                // che nella direzione opposta.
                                registeredList.get(us).setFriend(nfriend);
                                registeredList.get(nfriend).setFriend(us);
                                isok = 1;

                            }
                        } else {
                            isok= -1;
                        }
                    }

                    // invio risposta.
                    String risp ="";
                    /*
                    *   risp = 1: aggiungi amico ok;
                    *   risp = -1: amico non esistente;
                    *   risp = -2: amico precedentemente aggiunto.
                    *
                    * */

                    risp += isok;
                    Auxiliar con7 = (Auxiliar)key.attachment();

                    con7.resp = ByteBuffer.wrap(risp.getBytes());

                    try {
                        client.write(con7.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                    ServerService.saveUsersStats(registeredList);
                break;
                case "mostra_punteggio":
                    String ute = elenco[1];

                    long punteggio;

                    synchronized (registeredList){
                        punteggio= registeredList.get(ute).getPoint();
                    }

                    // una volta ottenuto il punteggio dell'utente invio risposta.
                    String risposta =""+ punteggio;
                    Auxiliar con6 = (Auxiliar)key.attachment();

                    con6.resp = ByteBuffer.wrap(risposta.getBytes());

                    try {
                        client.write(con6.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "mostra_classifica":
                    String ut = elenco[1];
                    // Inserisco dettagli amici dell'utente che ha richiesto l'op in una Map.
                    Map<String,Long> classifica = new TreeMap<>();

                    synchronized (registeredList){
                        classifica.put(ut,registeredList.get(ut).getPoint());

                        Vector<String> friends = registeredList.get(ut).getFriends();

                        for (String friend : friends) {
                            classifica.put(friend, registeredList.get(friend).getPoint());
                        }
                    }

                    // Riordino la Map classifica utilizzando entriesSortedByValues.
                    SortedSet<Map.Entry<String,Long>> result = entriesSortedByValues(classifica);

                    // trasformo la classifica in oggetto JSON e invio risposta.
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
                    Auxiliar con5 = (Auxiliar)key.attachment();

                    con5.resp = ByteBuffer.wrap(rispo.getBytes());
                    try {
                        client.write(con5.resp);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                break;
                case "sfida":
                    String uo = elenco[1];
                    String friend = elenco[2];
                    String respo = "";

                    Auxiliar con4= (Auxiliar)key.attachment();

                    int isafriend = 1;
                    // verifico che l'utente sfidato appartenga agli amici dell'utente sfidante.
                    synchronized (registeredList){
                        if(!registeredList.get(uo).getFriends().contains(friend)){
                            isafriend = 0;
                        }
                    }

                    if(isafriend == 0){
                        // invio messaggio d'errore e termino

                        respo += "Utente "+friend+" non è un tuo amico.";

                        con4.resp = ByteBuffer.wrap(respo.getBytes());
                        try {
                            client.write(con4.resp);
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    } else {
                        // invio sfida ad amico solo se è online.
                        // se non è online invio messaggio d'errore.
                        int error = 1;

                        SelectionKey amico = null;
                        synchronized (usersList) {
                            if (!usersList.containsKey(friend)) {
                                error = 0;
                            } else {
                                amico = usersList.get(friend);
                            }
                        }

                        if (error == 0) {

                            // invio messaggio d'errore amico sfidato non è online
                            respo += "Amico " + friend + " non è online.";

                            Auxiliar con3 = (Auxiliar) key.attachment();

                            con3.resp = ByteBuffer.wrap(respo.getBytes());
                            try {
                                client.write(con3.resp);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {

                            boolean contains;

                            // verifico che l'amico sfidato non sia impegnato in altra sfida.
                            synchronized (gamers){
                                contains = gamers.contains(friend);
                            }
                            if (contains) {
                                respo += "Amico " + friend + " sta già svolgendo una sfida";

                                Auxiliar con3 = (Auxiliar) key.attachment();

                                con3.resp = ByteBuffer.wrap(respo.getBytes());
                                try {
                                    client.write(con3.resp);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                // preparo tutto per inviare richiesta allo sfidato e mettere in attesa lo sfidante.
                                sfidante1 = key;
                                sfidato1 = amico;
                                synchronized (gamers) {
                                    gamers.add(friend);
                                    gamers.add(uo);
                                }


                                ServerService.dontRead(key, amico);

                                Random rand = new Random();

                                IntStream stream = rand.ints(13200, 15200);

                                try {

                                    // inizializzo selector e datagramchannel
                                    selector = Selector.open();
                                    DatagramChannel channel = DatagramChannel.open();

                                    Iterator<Integer> iterator1 = stream.iterator();
                                    int port = iterator1.next();

                                    InetSocketAddress isa = new InetSocketAddress(port);

                                    try {
                                        channel.socket().bind(isa);

                                    } catch (AlreadyBoundException s) {

                                        port = useAnotherPort(iterator1, channel);
                                    }
                                    channel.configureBlocking(false);

                                    respo += "Scrivi qui:" + port;
                                    Auxiliar con2 = (Auxiliar) key.attachment();

                                    con2.resp = ByteBuffer.wrap(respo.getBytes());
                                    try {
                                        client.write(con2.resp);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }


                                    SelectionKey clientkey;

                                    try {
                                        clientkey = channel.register(selector, SelectionKey.OP_READ);
                                        clientkey.attach(new Auxiliar(uo, friend, port,null));
                                    } catch (ClosedChannelException cce) {
                                        cce.printStackTrace();
                                    }

                                    ServerService.callBack(friend, port);
                                    sfidant = 5;
                                    sfidat = 5;

                                    // attendo i vari esiti della richiesta, se startingSfida = true inizia la sfida altrimenti
                                    // essa non è stata accettata o il timer dello sfidato è scaduto.
                                    while (!startingSfida && (sfidant == 5 && sfidat == 5)) {
                                        if (selector.isOpen()) {
                                            selector.select();
                                            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();

                                            while (selectedKeys.hasNext()) {
                                                SelectionKey key = selectedKeys.next();
                                                selectedKeys.remove();

                                               if (key.isReadable()) {
                                                   readUDPreq(key);
                                               } else if (key.isWritable()) {
                                                    sendUDPreq(key);
                                               }

                                            }
                                        }

                                    }
                                    if (startingSfida) {


                                        sfidant = 0;
                                        sfidat = 0;

                                        // seleziono le parole da dizionario e le traduco per iniziare sfida.

                                        int k_BOUND = 5;
                                        int K = Math.abs(rand.nextInt(k_BOUND)) + 1;
                                        Path path = Paths.get(".");
                                        Path JsonNioPath = path.resolve(dizionario);
                                        Vector<String> words = new Vector<>();
                                        if (Files.exists(JsonNioPath)) {
                                            String elements;
                                            try {

                                                elements = MainClassServer.readJson(dizionario);
                                                words = takeWordsFromJson(K, elements);

                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                        }

                                        Auxiliar aux1 = new Auxiliar(elenco[1], elenco[2], port,sfidato1);
                                        Auxiliar aux2 = new Auxiliar(elenco[1], elenco[2], port,sfidante1);

                                        aux1.setWords(words);
                                        aux2.setWords(words);

                                        Vector<String> traduzioni = translateWords(words);

                                        aux1.setWordsTradotte(traduzioni);
                                        aux2.setWordsTradotte(traduzioni);

                                        // invio tempo della sfida e prima parola ad entrambi.
                                        // lato client i due sfidanti setteranno il loro thread Timeout.
                                        String rispp = "60000/" + words.size() + "/" + words.get(0).trim();

                                        aux1.resp = ByteBuffer.wrap(rispp.getBytes());
                                        aux2.resp = ByteBuffer.wrap(rispp.getBytes());

                                        aux1.setId(1);
                                        aux2.setId(2);
                                        key.attach(aux1);
                                        amico.attach(aux2);

                                        SocketChannel can1 = (SocketChannel) sfidante1.channel();
                                        can1.write(aux1.resp);

                                        SocketChannel can2 = (SocketChannel) sfidato1.channel();

                                        can2.write(aux2.resp);

                                        sfidant = 5;
                                        sfidat = 5;
                                        counters.resetPoints(elenco[1]);
                                        counters.resetPoints(elenco[2]);

                                        // imposto in OP_READ le chiavi degli sfidanti.
                                        sfidante1.interestOps(SelectionKey.OP_READ);
                                        sfidato1.interestOps(SelectionKey.OP_READ);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                // timer durata sfida: 60 secondi.
                                // timer durata attesa per accettare sfida: 30 secondi.


                                try {
                                    selector.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                ServerService.abilityRead(key, amico);
                            }
                        }
                    }
                break;

                default:
                case "parola":{
                    // ricevuta parola successiva della sfida.
                    try {
                        tryReadTCP(key,receive);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                break;

            }
        }
    }

    /**
     * Metodo usato per ricavare risultati della sfida e aggiornare registeredList.
     * @param ok variabile usata per capire se è possibile inviare i risultati della sfida.
     * @param key chiave contenente info sugli sfidanti.
     */
    public void startResponse(int ok,SelectionKey key){


        Auxiliar temp = (Auxiliar) key.attachment();
        String sfidato = temp.getSfidato();
        String sfidante = temp.getSfidante();

        if(ok == 1){

            // calcolo risultati e invio risposto ai due giocatori.
            int[] punteggioSfidante = counters.getResults(sfidante);
            int[] punteggioSfidato = counters.getResults(sfidato);
            int puntsfidant = punteggioSfidante[3];
            int paroleOkSfidant = punteggioSfidante[0];
            int paroleNotOkSfidant = punteggioSfidante[1];
            int paroleNotTraSfidant = punteggioSfidante[2];

            int puntsfidat = punteggioSfidato[3];
            int paroleOkSfidat = punteggioSfidato[0];
            int paroleNotOkSfidat = punteggioSfidato[1];
            int paroleNotTraSfidat = punteggioSfidato[2];
            if(puntsfidat < puntsfidant){
                int finalpunt = puntsfidant+3;
                sendToWinner(sfidante1,temp, puntsfidant, paroleOkSfidant, paroleNotOkSfidant, paroleNotTraSfidant, puntsfidat, finalpunt);

                synchronized (registeredList){
                    registeredList.get(sfidante).setPoint(finalpunt);
                    registeredList.get(sfidato).setPoint(puntsfidat);
                }

                sendToLoser(sfidato1,puntsfidant, temp, puntsfidat, paroleOkSfidat, paroleNotOkSfidat, paroleNotTraSfidat);


            } else if(puntsfidant == puntsfidat){
                sendRisultato(sfidante1,temp, puntsfidant, paroleOkSfidant, paroleNotOkSfidant, paroleNotTraSfidant, puntsfidat);

                synchronized (registeredList){
                    registeredList.get(sfidante).setPoint(puntsfidant);
                    registeredList.get(sfidato).setPoint(puntsfidat);
                }

                sendRisultato(sfidato1,temp, puntsfidat, paroleOkSfidat, paroleNotOkSfidat, paroleNotTraSfidat, puntsfidant);

            } else {
                int finalpunt = puntsfidat+3;
                sendToLoser(sfidante1,puntsfidat, temp, puntsfidant, paroleOkSfidant, paroleNotOkSfidant, paroleNotTraSfidant);

                synchronized (registeredList){
                    registeredList.get(sfidante).setPoint(puntsfidant);
                    registeredList.get(sfidato).setPoint(finalpunt);
                }

                sendToWinner(sfidato1,temp, puntsfidat, paroleOkSfidat, paroleNotOkSfidat, paroleNotTraSfidat, puntsfidant, finalpunt);

            }
            synchronized (gamers){
                gamers.remove(sfidante);
                gamers.remove(sfidato);
            }
        } else {

            String respp = "non accettata";

            ByteBuffer writer = ByteBuffer.wrap(respp.getBytes());
            synchronized (gamers){
                gamers.remove(sfidante);
                gamers.remove(sfidato);
            }
            try {
                client.write(writer);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    /**
     * Gestisce setta i punteggi in base alla stringa ricevuta e in caso invia parola successiva.
     * @param key chiave dell'utente che sta svolgendo la sfida.
     * @param receive Stringa ricevuta dal client.
     * @throws IOException eccezione che potrebbe essere sollevata.
     */
    public void tryReadTCP(SelectionKey key,String receive)throws IOException{

        System.out.println("ecco stringa ricevuta: "+receive);
        String[] elenco = receive.split("/");
        boolean otherPlayerOnline = true;

        if(receive.contains("parola")){
            // leggo parola tradotta aggiorno punteggio e mando parola successiva
            // se ancora non sono finite altrimenti aspetto altro utente che finisca
            // per mandare i risultati della sfida.
            // parola/1/K/parolaTradotta

            int actual = Integer.parseInt(elenco[1]);
            int total = Integer.parseInt(elenco[2]);

            String word = elenco[3];

            // calcolo risultato.
            Auxiliar temp = (Auxiliar) key.attachment();
            int id = temp.getId();
            switch (id){
                case 1:
                    if(word.equals(" ")){
                        counters.setAbsent(temp.getSfidante());
                    } else if (temp.containsTrad(word)){
                        counters.setCorrect(temp.getSfidante());
                    } else {
                        counters.setUnCorrect(temp.getSfidante());
                    }
                    break;
                case 2:
                    if(word.equals(" ")){
                        counters.setAbsent(temp.getSfidato());
                    } else if (temp.containsTrad(word)){
                        counters.setCorrect(temp.getSfidato());
                    } else {
                        counters.setUnCorrect(temp.getSfidato());
                    }
                    break;
            }

            if(actual>=total){
                // Se uno dei due utenti ha terminato la sfida.
                key.attach(temp);

                switch (id){
                    case 1:
                        counters.setEndChallenge(temp.getSfidante());
                        synchronized (usersList){
                            otherPlayerOnline = usersList.containsKey(temp.getSfidato());
                        }
                        break;
                    case 2:
                        counters.setEndChallenge(temp.getSfidato());
                        synchronized (usersList){
                            otherPlayerOnline = usersList.containsKey(temp.getSfidante());
                        }
                        break;
                }
                endOne(key, temp, id);
                if(otherPlayerOnline) {

                    // se l'altro utente è online verifico se entrambi utenti abbiano finito la loro sfida e in caso invio risultati.
                    if (counters.getEndChallenge(temp.getSfidante()) == 1 && counters.getEndChallenge(temp.getSfidato()) == 1) {
                        startResponse(1, key);

                        ServerService.saveUsersStats(registeredList);
                    }
                } else {
                    // se l'altro utente non è più online annullo la sfida.
                    SocketChannel chan2 = (SocketChannel) key.channel();
                    ByteBuffer wrr = ByteBuffer.wrap("L'avversario si è ritirato".getBytes());
                    synchronized (gamers) {
                        gamers.remove(temp.getSfidato());
                        gamers.remove(temp.getSfidante());
                    }
                    try {
                        chan2.write(wrr);
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }

            } else {
                // l'utente non ha ancora terminato la sfida duqnue inoltro parola successiva.
                actual = actual+1;
                String newWord = temp.getWord(actual-1);
                String response = "parola/"+actual+"/"+total+"/"+newWord;
                temp.resp.clear();
                temp.resp = ByteBuffer.wrap(response.getBytes());
                key.attach(temp);
                client.write(temp.resp);
                key.interestOps(SelectionKey.OP_READ);
            }
        } else {
            // in caso di tempo scaduto.
            Auxiliar temp = (Auxiliar)key.attachment();
            int id = temp.getId();
            key.attach(temp);
            endOne(key,temp,id);

            switch (id){
                case 1:
                    counters.setEndChallenge(temp.getSfidante());
                    synchronized (usersList){
                        otherPlayerOnline = usersList.containsKey(temp.getSfidato());
                    }
                    break;
                case 2:
                    counters.setEndChallenge(temp.getSfidato());
                    synchronized (usersList){
                        otherPlayerOnline = usersList.containsKey(temp.getSfidante());
                    }
                    break;
            }

            if(otherPlayerOnline) {
                if (counters.getEndChallenge(temp.getSfidante()) == 1 && counters.getEndChallenge(temp.getSfidato()) == 1) {
                    startResponse(1, key);

                    ServerService.saveUsersStats(registeredList);
                }
            } else {

                SocketChannel chan2 = (SocketChannel) key.channel();
                ByteBuffer wrr = ByteBuffer.wrap("L'avversario si è ritirato".getBytes());
                synchronized (gamers) {
                    gamers.remove(temp.getSfidato());
                    gamers.remove(temp.getSfidante());
                }
                try {
                    chan2.write(wrr);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Metodo usato per inviare risultato allo sconfitto.
     * @param key chiave dello sconfitto.
     * @param puntsfidant punteggio del vincitore.
     * @param tempSfidato classe Auxiliar dello sfidato.
     * @param puntsfidat punteggio dello sfidato.
     * @param paroleOkSfidat numero parole tradotte correttamente.
     * @param paroleNotOkSfidat numero parole sbagliate.
     * @param paroleNotTraSfidat numero parole non tradotte.
     */
    private static void sendToLoser(SelectionKey key,int puntsfidant, Auxiliar tempSfidato, int puntsfidat, int paroleOkSfidat, int paroleNotOkSfidat, int paroleNotTraSfidat) {
        String risp2;
        risp2 = "Hai tradotto correttamente "+paroleOkSfidat+", ne hai sbagliate "+paroleNotOkSfidat+" e non hai risposto a "+paroleNotTraSfidat+"."
                +"\nHai totalizzato "+puntsfidat+" punti."+
                "\nIl tuo avversario ha totalizzato "+(puntsfidant +3)+" punti."+
                "\nHai perso la sfida...";
        tempSfidato.resp = ByteBuffer.wrap(risp2.getBytes());
        SocketChannel chan2 = (SocketChannel) key.channel();
        try {
            chan2.write(tempSfidato.resp);

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Manda risultato all'utente vincitore.
     * @param key chiave dell'utente vincitore.
     * @param tempSfidante classe Auxiliar dello sfidante.
     * @param puntsfidant punteggio del vincitore.
     * @param paroleOkSfidant numero parole tradotte correttamente.
     * @param paroleNotOkSfidant numero parole tradotte male.
     * @param paroleNotTraSfidant numero parole non tradotte.
     * @param puntsfidat punteggio dello sfidato sconfitto.
     * @param finalpunt punteggio finale del vincitore.
     */
    private static void sendToWinner(SelectionKey key, Auxiliar tempSfidante, int puntsfidant, int paroleOkSfidant, int paroleNotOkSfidant, int paroleNotTraSfidant, int puntsfidat, int finalpunt) {
        String risp1;
        risp1 = "Hai tradotto correttamente "+paroleOkSfidant+", ne hai sbagliate "+paroleNotOkSfidant+" e non hai risposto a "+paroleNotTraSfidant+"."
                +"\nHai totalizzato "+puntsfidant+" punti."+
                "\nIl tuo avversario ha totalizzato "+puntsfidat+" punti."+
                "\nCongratulazioni, hai vinto! Hai guadagnato 3 punti extra, per un totale di "+finalpunt+" punti!";
        tempSfidante.resp = ByteBuffer.wrap(risp1.getBytes());
        SocketChannel chan2 = (SocketChannel) key.channel();
        try {
            chan2.write(tempSfidante.resp);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Invia risultato ad uno dei due utenti che hanno pareggiato la sfida.
     * @param key chiave dell'utente al quale si vuole inviare il risultato della sfida.
     * @param tempSfidante classe Auxiliar dell'utente descritto su.
     * @param puntsfidant punteggio dell'utente.
     * @param paroleOkSfidant numero parole tradotte correttamente.
     * @param paroleNotOkSfidant numero parole tradotte male.
     * @param paroleNotTraSfidant numero parole non tradotte.
     * @param puntsfidat punteggio dell'altro utente.
     */
    private static void sendRisultato(SelectionKey key, Auxiliar tempSfidante, int puntsfidant, int paroleOkSfidant, int paroleNotOkSfidant, int paroleNotTraSfidant, int puntsfidat) {
        String risp1;
        risp1 = "Hai tradotto correttamente "+paroleOkSfidant+", ne hai sbagliate "+paroleNotOkSfidant+" e non hai risposto a "+paroleNotTraSfidant+"."
                +"\nHai totalizzato "+puntsfidant+" punti."+
                "\nIl tuo avversario ha totalizzato "+puntsfidat+" punti."+
                "\nLa sfida è finita in parità";
        tempSfidante.resp = ByteBuffer.wrap(risp1.getBytes());
        SocketChannel chan2 = (SocketChannel) key.channel();
        try {
            chan2.write(tempSfidante.resp);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Metodo usato per ordinare gli utenti amici del client che ha richiesto classifica in base al punteggio.
     * @param map Map contenente elementi da ordinare.
     * @param <K> generico della chiave della map.
     * @param <V> generico del valore della map.
     * @return un SortedSet sul valore degli elementi di map.
     */
    static <K,V extends Comparable<? super V>>
    SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<>(
                (e1, e2) -> {
                    int res = e2.getValue().compareTo(e1.getValue());
                    return res != 0 ? res : 1;
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }


    /**
     * Classe Auxiliar verrà messa in attachment a tutte le chiavi dei client che si connetteranno con il server.
     */
    public static class Auxiliar {

        ByteBuffer req;
        // ByteBuffer contenente richiesta.

        ByteBuffer resp;
        // ByteBuffer contenente risposta

        SocketAddress sa;
        // SocketAddress del client.

        Vector<String> wordsDatradurre;
        // elenco parole da tradurre in caso di sfida.

        Vector<String> wordsTradotte;
        // elenco parole tradotte.

        String sfidante;
        // nome dello sfidante in caso di sfida.

        String sfidato;
        // nome dello sfidato.

        // variabili ausiliarie.
        int porta;
        int fine;
        SelectionKey altro;

        // id dell'utente in questione.
        // serve a capire se sia lo sfidante o lo sfidato.
        private int id;
        /*
         *   id= 1 : sfidante;
         *   id= 2 : sfidato;
         *
         * */

        public Auxiliar(String sfidante,String sfidato,int porta,SelectionKey altro) {
            int BUF_DIM = 1024;
            req = ByteBuffer.allocate(BUF_DIM);
            this.sfidante = sfidante;
            this.sfidato = sfidato;
            this.porta = porta;
            id = 0;
            fine = 0;
            this.altro = altro;
        }

        public void clearAll(){
            req.clear();
            resp.clear();
        }


        public String getSfidante(){
            return sfidante;
        }
        public String getSfidato(){
            return sfidato;
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

    }

    /**
     * Metodo per gestire le richieste UDP successive all'invio della richiesta di sfida.
     * @param key chiave dell'utente che è stato sfidato.
     * @throws IOException eccezione che si potrebbe sollevare.
     */
    private void readUDPreq(SelectionKey key) throws IOException {
        DatagramChannel chan = (DatagramChannel) key.channel();
        Auxiliar aux = (Auxiliar) key.attachment();
        aux.sa = chan.receive(aux.req);

        aux.req.flip();
        aux.resp = aux.req;

        String req = StandardCharsets.UTF_8.decode(aux.resp).toString();
        System.out.println("ecco stringa ricevuta: "+req);
        String[] elenco = req.split("/");
        String send;
        switch (elenco[0]){
            case "sfidato":
                send = "Sei stato sfidato da "+aux.sfidante+" vuoi accettare?/10";
                aux.resp = ByteBuffer.wrap(send.getBytes());
                key.attach(aux);
                aux.setId(2);
                key.attach(aux);
                key.interestOps(SelectionKey.OP_WRITE);
                break;
            case "sfidante":
                send = "In attesa dello sfidato: "+aux.sfidato;
                aux.resp = ByteBuffer.wrap(send.getBytes());
                key.attach(aux);
                sfidante = key;
                aux.setId(1);

                sfidante.attach(aux);
                sfidante.interestOps(SelectionKey.OP_WRITE);
                break;
            case "ok":

                startingSfida=true;
                key.interestOps(SelectionKey.OP_READ);
                sfidante.interestOps(SelectionKey.OP_READ);
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
                
            default:
                key.interestOps(SelectionKey.OP_WRITE);
                break;
        }

    }

    /**
     * Imposta a 1 variabile ausiliaria fine, e soprattutto aggiorna key sfidante1 e sfidato1.
     * @param key chiave dell'utente che ha appena terminato la sua sfida.
     * @param temp1 classe Auxiliar dell'utente descritto su.
     * @param id1 id dell'utente descritto su.
     */
    private void endOne(SelectionKey key, Auxiliar temp1, int id1) {
        switch (id1){
            case 1:
                sfidant = 0;
                sfidante1 = key;

                temp1.fine = 1;
                sfidante1.attach(temp1);
                break;
            case 2:
                sfidat = 0;
                sfidato1 = key;
                temp1.fine = 1;
                sfidato1.attach(temp1);
                break;
        }
    }

    /**
     * Traduce le parole della sfida con chiamata REST.
     * @param words elenco di parole da tradurre.
     * @return Vector contenente le parole della sfida tradotte..
     */
    private Vector<String> translateWords(Vector<String> words){
        Vector<String> tradotte = new Vector<>();

        for (String word : words) {
            try {
                URL url1 = new URL("https://api.mymemory.translated.net/get?q=" + word + "&langpair=it|en");


                try (BufferedReader in = new BufferedReader(new InputStreamReader(url1.openStream()))) {
                    StringBuilder inputLine = new StringBuilder();
                    String reader;

                    while ((reader = in.readLine()) != null) {
                        inputLine.append(reader);
                    }

                    JSONObject jsonObject;
                    JSONParser parser = new JSONParser();

                    try {
                        jsonObject = (JSONObject) parser.parse(inputLine.toString());

                        JSONArray array = (JSONArray) jsonObject.get("matches");

                        for (Object o : array) {
                            JSONObject obj = (JSONObject) o;
                            String stampa1 = (String) obj.get("translation");
                            tradotte.add(stampa1);
                        }
                    } catch (ParseException e) {
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

    /**
     * Metodo usato per inviare le risposte in UDP in fase di accettazione sfida.
     * @param key chiave dell'utente al quale rispondere
     */
    private void sendUDPreq(SelectionKey key){
        DatagramChannel chan = (DatagramChannel) key.channel();
        Auxiliar aux = (Auxiliar) key.attachment();
        String risp = StandardCharsets.UTF_8.decode(aux.resp).toString();

        if(!risp.contains("parola")){
            try {

                aux.resp.flip();
                System.out.println(StandardCharsets.UTF_8.decode(aux.resp).toString());

                aux.resp.flip();
                chan.send(aux.resp, aux.sa);


            } catch (IOException e){
                e.printStackTrace();
            }
            if(risp.contains("sfida non accettata")){

                startResponse(0,key);
                sfidant = 2;
                sfidat = 2;
                key.cancel();
                try {
                    selector.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            } else {

                aux.resp.clear();
                aux.req.clear();
                key.attach(aux);
                key.interestOps(SelectionKey.OP_READ);
            }
        } else {
            if(risp.contains("In attesa dello sfidato")){
                try {
                    aux.resp.flip();
                    chan.send(aux.resp, aux.sa);
                } catch (IOException e){
                    e.printStackTrace();
                }
            } else {
                try {
                    aux.resp.flip();

                    chan.send(aux.resp, aux.sa);
                    aux.resp.clear();
                    aux.req.clear();
                    key.attach(aux);
                    key.interestOps(SelectionKey.OP_READ);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param iterator Iteratore contenente numeri di porta sui quali far connettere in UDP il client sfidato e lo sfidante
     *                 solo in fase di accettazione sfida.
     * @param channel DatagramChannel sul quale fare bind della porta.
     * @return la porta selezionata correttamente.
     */
    private int useAnotherPort(Iterator<Integer> iterator,DatagramChannel channel){
        boolean findIt = false;
        int nextPort =13201;
        while(!findIt){
            nextPort = iterator.next();
            try {
                InetSocketAddress isa = new InetSocketAddress(nextPort);

                channel.socket().bind(isa);

                findIt = true;
            } catch (Exception a){
                a.printStackTrace();
            }
        }
        return nextPort;
    }

    /**
     * Metodo usato per selezionare le K parole della sfida.
     * @param k numero di parole da selezionare per la sfida.
     * @param json stringa json contenente tutte le parole presenti nel file Dizionario.json.
     * @return Vector contenente parole della sfida.
     */
    private Vector<String> takeWordsFromJson(int k,String json){
        Vector<String> result = new Vector<>();
        Vector<String> dictionary = new Vector<>();
        JSONArray jsonArray;
        JSONParser parser = new JSONParser();

        try{
            jsonArray = (JSONArray) parser.parse(json);
            for (Object obj : jsonArray) {
                JSONObject obj2 = (JSONObject) obj;
                String word = (String) obj2.get("word");
                dictionary.add(word);

            }

            while(result.size()<k){
                Random rand = new Random();
                int index = rand.nextInt(dictionary.size());

                if(!result.contains(dictionary.get(index))){
                    result.add(dictionary.get(index));

                }
            }
        }catch (ParseException e){
            e.printStackTrace();
        }
        return result;
    }
}
