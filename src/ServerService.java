import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerService implements Runnable {

    private static int BUF_SZ = 1024;
    private static String fileJsonName;
    private int port;
    private TreeMap<String, Utente> registeredList;
    private TreeMap<String,SelectionKey> usersList;
    private ThreadPoolExecutor threadPoolExecutor;

    public ServerService(int port, TreeMap<String, Utente> albero, TreeMap<String,SelectionKey> online, String fileJsonName) {
        this.port = port;
        registeredList = albero;
        this.fileJsonName = fileJsonName;
        usersList = online;
        LinkedBlockingQueue<Runnable> linkedlist = new LinkedBlockingQueue<>();
        threadPoolExecutor = new ThreadPoolExecutor(0,5,500, TimeUnit.MILLISECONDS,linkedlist);
    }


    public void run() {

        System.out.println("Listening for connection on port "+port+" ...");

        ServerSocketChannel serverChannel;
        Selector selector;

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
            saveUsersStats(registeredList);

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
                        //server.configureBlocking(false);

                        SocketChannel client = server.accept();

                        // imposto connessione non bloccante
                        client.configureBlocking(false);

                        // registro OP_READ ke
                        if(client != null) {
                            SelectionKey clientkey = client.register(selector, SelectionKey.OP_READ);
                            clientkey.attach(new Con());
                        }


                    }

                    // se chiave è Readable
                    if (key.isReadable()) {
                        try {
                            tryRead(key);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        //key.interestOps(SelectionKey.OP_WRITE);
                    } else if (key.isWritable()) {

                        makeWrite(key);

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
            saveUsersStats(registeredList);
        }catch ( IOException e) {
            saveUsersStats(registeredList);

        }

        System.out.println("Chiusura ServerService ...");

    }

    public class Con {

        ByteBuffer req;
        ByteBuffer resp;
        SocketAddress sa;

        /*
        *   type 1 = login
        *   type 2 = logout
        *
        * */
        int typeOp;

        public Con() {
            typeOp = -1;
            req = ByteBuffer.allocate(BUF_SZ);
        }

        public void clearAll(){
            req.clear();
            resp.clear();
            typeOp = -1;
        }
    }


    private void tryRead(SelectionKey key) throws Exception{

        // seleziono channel e leggo
        SocketChannel client = (SocketChannel) key.channel();
        Con con = (Con) key.attachment();

        try {
            con.sa = client.getRemoteAddress();
        }catch (IOException e){
            e.printStackTrace();
        }

        con.req.clear();

        int bytes= 0;
        try {
            bytes= client.read(con.req);
        }catch (IOException e){
            e.printStackTrace();
        }

        if(bytes >0) {

            Worker work = new Worker(con, registeredList, usersList,key);
            threadPoolExecutor.execute(work);
        }
    }

    public static void tryWrite(SelectionKey key1,SelectionKey key2){

        key1.interestOps(0);
        key2.interestOps(0);

    }

    private void makeWrite(SelectionKey key){
        SocketChannel client =(SocketChannel) key.channel();
        Con con = (Con) key.attachment();
        try{
            client.write(con.resp);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void saveUsersStats(TreeMap<String, Utente> registeredList){

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

            // scrivo il file nuo con json object
            FileChannel outChannel = FileChannel.open(JsonNioPath, StandardOpenOption.WRITE);
            String body = array.toJSONString();
            byte[] bytes = body.getBytes();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            outChannel.write(buf);
            outChannel.close();
        } catch (IOException e){
            e.printStackTrace();
            return;
        }

    }

}




