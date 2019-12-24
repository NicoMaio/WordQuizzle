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

public class ServerService implements Runnable {

    private static int BUF_SZ = 1024;
    private String fileJsonName;
    private int port;
    private TreeMap<String,Elemento> registeredList;

    public ServerService(int port, TreeMap<String,Elemento> albero, String fileJsonName) {
        this.port = port;
        registeredList = albero;
        this.fileJsonName = fileJsonName;
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

                        // imposto connessione non bloccante
                        client.configureBlocking(false);

                        // registro OP_READ key
                        SelectionKey clientkey = client.register(selector, SelectionKey.OP_READ);
                        clientkey.attach(new Con());

                    }

                    // se chiave è Readable
                    if (key.isReadable()) {

                        // tryRead(key);

                    } else if (key.isWritable()) {

                        // tryWrite(key);
                    }
                }

            } catch (IOException e) {

                    System.err.println("Errore: " + e);
            }

        }

        try {
            selector.close();
            serverChannel.close();
            saveUsersStats(registeredList);
        }catch ( IOException e) {
        }

        System.out.println("Chiusura ServerService ...");

    }

    class Con {

        ByteBuffer req;
        ByteBuffer resp;
        SocketAddress sa;

        public Con() {
            req = ByteBuffer.allocate(BUF_SZ);
        }
    }



    private void saveUsersStats(TreeMap<String,Elemento> registeredList){

        Set<Map.Entry<String,Elemento>> set = registeredList.entrySet();
        Iterator<Map.Entry<String,Elemento>> iterator=    set.iterator();

        JSONArray array= new JSONArray();

        while(iterator.hasNext()){
            JSONObject obj = new JSONObject();
            Elemento temp = iterator.next().getValue();
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




