/**
 * @author Nicolò Maio
 *
 * Task eseguito dal thread lanciato dal server che gestisce le varie richieste dei client.
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
    //
    private Counters counters;

    public ServerService(int port, TreeMap<String, Utente> albero, TreeMap<String,SelectionKey> online,
                         String fileJsonName,ServerCallBImpl server, Counters counters) {
        this.port = port;
        registeredList = albero;
        ServerService.fileJsonName = fileJsonName;
        usersList = online;
        this.counters = counters;
        gamers = new Vector<>();
        LinkedBlockingQueue<Runnable> linkedlist = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(0,30,500, TimeUnit.MILLISECONDS,linkedlist);
        ServerService.server = server;
    }

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


    public void run() {

        System.out.println("Listening for connection on port "+port+" ...");

        ServerSocketChannel serverChannel;
        //Selector selector;

        try {

            // apro Worker.Auxiliarnessione
            serverChannel = ServerSocketChannel.open();
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);

            ss.bind(address);

            // imposto Worker.Auxiliarnessione non bloccante lato server
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

                        // accetto Worker.Auxiliarnessione
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        //server.Worker.AuxiliarfigureBlocking(false);

                        SocketChannel client = server.accept();

                        // imposto Worker.Auxiliarnessione non bloccante
                        client.configureBlocking(false);

                        // registro OP_READ ke
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

        }

        System.out.println("Chiusura ServerService ...");

    }




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

    public static void dontRead(SelectionKey key1,SelectionKey key2){

        key1.interestOps(SelectionKey.OP_WRITE);
        key2.interestOps(SelectionKey.OP_WRITE);

    }

    public static void abilityRead(SelectionKey key1, SelectionKey key2){
        System.out.println("Riattivate");
        key1.interestOps(SelectionKey.OP_READ);
        key2.interestOps(SelectionKey.OP_READ);

        selector.wakeup();


    }

    public  void saveUsersStats(){

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

    public static void callBack(String username,int value) throws RemoteException {

        server.update(value,username);
    }
}




