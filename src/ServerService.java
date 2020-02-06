/**
 * @author Nicolò Maio
 *
 * Task eseguito dal thread lanciato dal server che gestisce le varie richieste dei client, tramite Selector
 * */

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerService implements Runnable {

    private static String fileJsonName;
    // String contente nome del file json su cui salvare update di registeredList.

    private int port;
    // porta sulla quale si riceveranno le varie richieste dei client.

    private final TreeMap<String, Utente> registeredList;
    // TreeMap registerdList conterrà tutte le info degli utenti registrati.

    private TreeMap<String,SelectionKey> usersList;
    // TreeMap conterrà elenco utenti online e rispettive SelectionKey.

    private Vector<String> gamers;
    // Vector degli utenti che stanno svolgendo delle sfide.

    private ThreadPoolExecutor threadPoolExecutor;
    // ThreadPoolExecutor usato per gestire le varie richieste dei client.

    private static ServerCallBImpl server;
    // ServerCallBImpl usata per gestire le callback lato server in caso di notifiche di sfida.

    private static Selector selector;
    // Selector per gestire le richieste dei client.

    private Counters counters;
    // Counters contenente i contatori delle sfide degli utenti.

    public ServerService(int port, TreeMap<String, Utente> albero, TreeMap<String,SelectionKey> online,
                         String fileJsonName,ServerCallBImpl server, Counters counters) {
        this.port = port;
        registeredList = albero;
        ServerService.fileJsonName = fileJsonName;
        usersList = online;
        this.counters = counters;
        gamers = new Vector<>();
        ServerService.server = server;

        // imposto ThreadPoolExecutor al quale verranno passate le richieste da svolgere.
        LinkedBlockingQueue<Runnable> linkedlist = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(0,30,500, TimeUnit.MILLISECONDS,linkedlist);

    }


    /**
     * Metodo run del thread su cui gira il selector.
     */
    public void run() {

        System.out.println("Listening for connection on port "+port+" ...");

        ServerSocketChannel serverChannel;

        try {

            // apro connessione
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);

            ss.bind(address);

            // imposto connessione non bloccante lato server
            serverChannel.configureBlocking(false);

            // apro selettore
            selector = Selector.open();

            // registro in selector chiave OP_ACCEPT
            serverChannel.register(selector,SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
            saveUsersStats();

            return;
        }


        while (!Thread.interrupted()) {
            try {

                // seleziono set di chiavi
                selector.select();

                // istanzio iteratore su selected-key set
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeySet.iterator();

                // scorro iteratore
                while (iterator.hasNext()) {

                    SelectionKey key = iterator.next();
                    iterator.remove();


                    // se chiave è Acceptable
                    if (key.isAcceptable()) {

                        // accetto connessione
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();

                        // imposto connessione  non bloccante
                        client.configureBlocking(false);

                        // registro OP_READ.
                        SelectionKey clientkey = client.register(selector, SelectionKey.OP_READ);
                        clientkey.attach(new Worker.Auxiliar("","",0,null));


                    }

                    // se chiave è Readable
                    if (key.isReadable()) {
                            tryRead(key);

                        //key.interestOps(SelectionKey.OP_WRITE);
                    }
                }

            } catch (IOException e) {

                    System.err.println("Errore: " + e);
            }

        }

        /* ---------- Protocollo di terminazione ---------- */
        try {
            selector.close();
            serverChannel.close();

            threadPoolExecutor.shutdown();
            while(!threadPoolExecutor.isTerminated()){

                try {
                    threadPoolExecutor.awaitTermination(Long.MAX_VALUE,TimeUnit.MILLISECONDS);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            saveUsersStats();
        }catch ( IOException e) {
            saveUsersStats();
            e.printStackTrace();

        }

        System.out.println("Chiusura ServerService ...");
        /* ------------------------------------------------ */

    }


    /**
     * Legge richiesta e la consegna al threadpool.
     * @param key chiave su cui è stato registrato connessione della prossima richiesta da svolgere.
     */
    private void tryRead(SelectionKey key) {

        // seleziono channel e leggo
        SocketChannel client = (SocketChannel) key.channel();
        Worker.Auxiliar aux = null;

        try {

            aux = (Worker.Auxiliar) key.attachment();
        } catch (ClassCastException e){
            e.printStackTrace();
        }

        int bytes= 0;
        if(aux!=null) {
            try {
                aux.sa = client.getRemoteAddress();
            } catch (IOException e) {
                e.printStackTrace();
            }

            aux.req.clear();

            try {
                bytes = client.read(aux.req);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            assert false;
            aux.req.clear();

            try{
                bytes= client.read(aux.req);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        if(bytes >0) {

            Worker work = new Worker(registeredList, usersList,key,gamers,counters);
            threadPoolExecutor.execute(work);
        }
    }

    /**
     * Disabilita le chiavi di due utenti che si stanno sfidando.
     * @param key1 chiave del primo utente della sfida.
     * @param key2 chiave del secondo utente della sfida.
     */
    public static void dontRead(SelectionKey key1,SelectionKey key2){

        key1.interestOps(SelectionKey.OP_WRITE);
        key2.interestOps(SelectionKey.OP_WRITE);

    }

    /**
     * Riattiva le chiavi dei due utenti una volta finita la sfida.
     * @param key1 chiave del primo utente che ha finito la sfida.
     * @param key2 chiave del secondo utente che ha finito la sfida.
     */
    public static void abilityRead(SelectionKey key1, SelectionKey key2){

        key1.interestOps(SelectionKey.OP_READ);
        key2.interestOps(SelectionKey.OP_READ);

        selector.wakeup();

    }

    /**
     * Salva sul file BackupServer.json le info contenute in registeredList.
     * @param registeredList TreeMap su cui si tiene traccia di tutte le info degli utenti registrati.
     */
    public static void saveUsersStats(TreeMap<String,Utente> registeredList) {
        Set<Map.Entry<String, Utente>> set;
        synchronized (registeredList) {
            set = registeredList.entrySet();
        }
        Iterator<Map.Entry<String, Utente>> iterator= set.iterator();

        JSONArray array= new JSONArray();

        while(iterator.hasNext()){
            JSONObject obj = new JSONObject();
            Utente temp = iterator.next().getValue();
            obj.put("username",temp.getUsername());
            obj.put("password",temp.getPassword());
            obj.put("points",temp.getPoint());

            Vector<String> friends =  temp.getFriends();
            Iterator<String> iterFriends = friends.iterator();
            JSONArray arrayfriends = new JSONArray();
            while(iterFriends.hasNext()){
                String temp2 = iterFriends.next();
                JSONObject obj2 = new JSONObject();
                obj2.put("username",temp2);
                arrayfriends.add(obj2);
            }
            obj.put("friends",arrayfriends);
            array.add(obj);

        }

        Path path = Paths.get(".");
        Path JsonNioPath = path.resolve(fileJsonName);

        try {

            // creo file nio
            Files.deleteIfExists(JsonNioPath);
            Files.createFile(JsonNioPath);

            // scrivo il file nuo Worker.Auxiliar json object
            FileChannel outChannel = FileChannel.open(JsonNioPath, StandardOpenOption.WRITE);
            String body = array.toJSONString();
            byte[] bytes = body.getBytes();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            outChannel.write(buf);
            outChannel.close();
        } catch (IOException e){
            e.printStackTrace();

        }
    }

    /**
     * Salva sul file BackupServer.json le info contenute in registeredList.
     */
    public void saveUsersStats(){

        Set<Map.Entry<String, Utente>> set;
        synchronized (registeredList) {
            set = registeredList.entrySet();
        }
        Iterator<Map.Entry<String, Utente>> iterator= set.iterator();

        JSONArray array= new JSONArray();

        while(iterator.hasNext()){
            JSONObject obj = new JSONObject();
            Utente temp = iterator.next().getValue();
            obj.put("username",temp.getUsername());
            obj.put("password",temp.getPassword());
            obj.put("points",temp.getPoint());

            Vector<String> friends =  temp.getFriends();
            Iterator<String> iterFriends = friends.iterator();
            JSONArray arrayfriends = new JSONArray();
            while(iterFriends.hasNext()){
                String temp2 = iterFriends.next();
                JSONObject obj2 = new JSONObject();
                obj2.put("username",temp2);
                arrayfriends.add(obj2);
            }
            obj.put("friends",arrayfriends);
            array.add(obj);

        }

        Path path = Paths.get(".");
        Path JsonNioPath = path.resolve(fileJsonName);

        try {

            // creo file nio
            Files.deleteIfExists(JsonNioPath);
            Files.createFile(JsonNioPath);

            // scrivo il file nuo Worker.Auxiliar json object
            FileChannel outChannel = FileChannel.open(JsonNioPath, StandardOpenOption.WRITE);
            String body = array.toJSONString();
            byte[] bytes = body.getBytes();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            outChannel.write(buf);
            outChannel.close();
        } catch (IOException e){
            e.printStackTrace();

        }

    }

    /**
     * Serve per notificare la sfida all'utente sfidato tramite RMI CallBack.
     * @param username username dell'utente a cui si notificherà la sfida.
     * @param value la porta che dovrà usare l'utente per rispondere alla richiesta inviata in UDP.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    public static void callBack(String username,int value) throws RemoteException {

        server.update(value,username);
    }
}




