import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class Worker implements Runnable {

    private final TreeMap<String, Utente> registeredList;
    private TreeMap<String, SelectionKey> usersList;
    private ServerService.Con con;
    private SelectionKey key;
    private Selector selector;
    private SelectionKey sfidante;
    private SelectionKey sfidato;

    private static int BUF_DIM = 1024;
    public Worker(ServerService.Con con,TreeMap<String, Utente> registeredList,
                  TreeMap<String, SelectionKey> usersList,SelectionKey key) {
        this.con  = con;
        this.registeredList = registeredList;
        this.usersList = usersList;
        this.key = key;
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

                            ServerService.tryWrite(key,amico);

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

                                respo += "Scrivi qui:"+port;

                                con.resp = ByteBuffer.wrap(respo.getBytes());
                                try {
                                    client.write(con.resp);
                                } catch (IOException e){
                                    e.printStackTrace();
                                }

                                // configuro non bloccante il DatagramChannel
                                channel.configureBlocking(false);

                                SelectionKey clientkey;

                                try {
                                    clientkey = channel.register(selector,SelectionKey.OP_READ);
                                    clientkey.attach(new auxiliar(uo,friend));
                                } catch (ClosedChannelException cce){
                                    cce.printStackTrace();
                                }
                                boolean endingSfida = false;
                                while(!endingSfida){
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

    public class auxiliar {

        ByteBuffer req;
        ByteBuffer resp;
        SocketAddress sa;
        Vector<String> wordsDatradurre;
        String sfidante;
        String sfidato;

        /*
         *   type 1 = login
         *   type 2 = logout
         *
         * */

        public auxiliar(String sfidante,String sfidato) {
            req = ByteBuffer.allocate(BUF_DIM);
            this.sfidante = sfidante;
            this.sfidato = sfidato;
        }

        public void clearAll(){
            req.clear();
            resp.clear();
        }

        public void setWords(Vector<String> parole){
            wordsDatradurre = parole;
        }
    }

    private void readUDPreq(SelectionKey key) throws IOException {
        DatagramChannel chan = (DatagramChannel) key.channel();
        auxiliar aux = (auxiliar) key.attachment();
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
                break;
            case "not ok":

                break;

            case "tempo scaduto per accettare":
                break;

            case "parola":
                break;
        }

    }

    private void sendUDPreq(SelectionKey key){
        DatagramChannel chan = (DatagramChannel) key.channel();
        auxiliar aux = (auxiliar) key.attachment();

        String risp = StandardCharsets.UTF_8.decode(aux.resp).toString();
        if(!risp.contains("parola")){
            try {
                chan.send(aux.resp, aux.sa);
            } catch (IOException e){
                e.printStackTrace();
            }
            key.interestOps(SelectionKey.OP_READ);
        } else {
            //
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
}
