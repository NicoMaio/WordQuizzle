import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class ServerClass {

    public static int DEFAULT_PORT = 13200;
    public static String fileJsonName = "BackupServer.json";

    public static void main(String[] args){
        // ServerClass [port]
        // [port]: numero di porta su cui il server resta in attesa

        TreeMap<String,Elemento> registeredList = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareTo(s2);
            }
        });

        TreeMap<String, SocketAddress> usersList = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareTo(s2);
            }
        });

        Path path = Paths.get(".");
        Path JsonNioPath = path.resolve(fileJsonName);
        if(Files.exists(JsonNioPath)){
            String elements;
            try{

                elements = readJson();
                buildRegistered(registeredList,elements);

            } catch (IOException e){
                e.printStackTrace();
            }

        }
        try {


            ImplRemoteRegistration register = new ImplRemoteRegistration(registeredList);
            LocateRegistry.createRegistry(9999);

            Registry r = LocateRegistry.getRegistry(9999);
            r.rebind(ImplRemoteRegistration.SERVICE_NAME, register);
        } catch ( RemoteException r ){
            r.printStackTrace();
        }


        int port;

        try {

            port = Integer.parseInt(args[0]);

        } catch (RuntimeException e) {

            port = DEFAULT_PORT;
        }


        Thread thread = new Thread(new ServerService(port,registeredList,usersList,fileJsonName));

        thread.start();

        boolean term = false;
        Scanner sc = new Scanner(System.in);
        while(!term) {

            if (sc.nextLine().equals("termina")){
                term = true;
            }

        }

        try {
            thread.interrupt();

            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sc.close();
        System.out.println("Server terminato...");

    }

    public static String readJson() throws IOException{
        Path path = Paths.get(".");
        Path JsonNioPath = path.resolve(fileJsonName);
        String result="";
        FileChannel inChannel = FileChannel.open(JsonNioPath, StandardOpenOption.READ);
        ByteBuffer byteBufferReader = ByteBuffer.allocate(1024 * 1024);
        boolean stop = false;
        while (!stop)
        {
            int bytesRead = inChannel.read(byteBufferReader);
            if (bytesRead == -1) stop = true;
            else
            {
                String tmp = new String(byteBufferReader.array(), 0, byteBufferReader.position());
                result += tmp;
            }
            byteBufferReader.clear();
        }
        inChannel.close();

        return result;
    }

    public static void buildRegistered(TreeMap<String,Elemento> registeredList, String result){

        JSONArray jsonArray;
        JSONParser parser = new JSONParser();

        try{
            jsonArray = (JSONArray) parser.parse(result);
            // ottengo array con tutti gli utenti

            Iterator<JSONObject> iterator = jsonArray.iterator();
            while(iterator.hasNext()){
                JSONObject obj = iterator.next();
                String username =(String)obj.get("username");
                String password =(String)obj.get("password");
                int point = (Integer)obj.get("points");

                Elemento elemento = new Elemento(username,password,point);
                JSONArray listaF = (JSONArray)obj.get("friends");
                Iterator<JSONObject> iterator1 = listaF.iterator();
                while(iterator1.hasNext()){
                    elemento.setFriend(iterator1.next().get("username").toString());
                }
                registeredList.put(elemento.getUsername(),elemento);

            }
        }catch (ParseException e){
            e.printStackTrace();
        }
    }

}
