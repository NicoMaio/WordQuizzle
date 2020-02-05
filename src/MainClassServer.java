/**
 * @author Nicolò Maio
 *
 * MainClassServer: Classe principale del Server di WordQuizzle.
 *
 * */

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * MainClassServer Classe principale del server di WordQuizzle.
 */
public class MainClassServer {

    public static int DEFAULT_PORT = 13200;
    // numero di default della porta su cui attenderà il server.

    public static String fileJsonName = "BackupServer.json";
    // nome del file json su cui verrà fatto il backup dell'intera struttura contenente i dati degli utenti.

    public static int PORT_FOR_REGISTRATIONS = 9999;
    // numero di porta per servizio RMI che gestisce l'operazione di registrazione.

    public static int PORT_FOR_CALLBACK = 5000;
    // numero di porta per servizio RMI callback per notificare utente sfidato.

    public static void main(String[] args){
        // MainClassServer [port]
        // [port]: numero di porta su cui il server resta in attesa, se non viene specificato si usa DEFAULT_PORT

        // TreeMap registeredList usata per gestire gli utenti registrati.
        TreeMap<String, Utente> registeredList = new TreeMap<>(String::compareTo);

        // TreeMap userList usata per gestire gli utenti online.
        TreeMap<String, SelectionKey> usersList = new TreeMap<>(String::compareTo);

        // Classe Counters usata per gestire i conteggi delle sfide
        Counters counters = new Counters();

        /*  -------- Recupero dati da file BackupServer.json -------- */

        Path path = Paths.get(".");
        Path JsonNioPath = path.resolve(fileJsonName);

        if(Files.exists(JsonNioPath)){

            String elements;

            try{

                elements = readJson(fileJsonName);
                buildRegistered(registeredList,elements);
                buildCounters(counters,elements);

            } catch (IOException e){
                e.printStackTrace();
            }

        }

        /*  --------------------------------------------------------- */

        /*  -------- Imposto servizio di registrazione con RMI -------- */
        try {

            ImplRemoteRegistration register = new ImplRemoteRegistration(registeredList,counters);
            LocateRegistry.createRegistry(PORT_FOR_REGISTRATIONS);

            Registry r = LocateRegistry.getRegistry(PORT_FOR_REGISTRATIONS);
            r.rebind(ImplRemoteRegistration.SERVICE_NAME, register);
        } catch ( RemoteException r ){
            r.printStackTrace();
        }
        /*  ----------------------------------------------------------- */

        /*  -------- Imposto servizio di callback con RMI per notificare utenti sfidati -------- */
        ServerCallBImpl server = null;
        Registry registry;

        try {

            server = new ServerCallBImpl();
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server,39000);
            LocateRegistry.createRegistry(PORT_FOR_CALLBACK);
            registry = LocateRegistry.getRegistry(PORT_FOR_CALLBACK);
            registry.bind(ServerInterface.SERVICE_NAME,stub);

        } catch (RemoteException | AlreadyBoundException | java.rmi.AlreadyBoundException r) {
            r.printStackTrace();
        }
        /*  ------------------------------------------------------------------------------------ */


        /*  -------- Ricavo porta per le connessioni TCP -------- */
        int port;

        try {

            port = Integer.parseInt(args[0]);

        } catch (RuntimeException e) {

            port = DEFAULT_PORT;
        }
        /*  ----------------------------------------------------- */


        // Lancio thread che eseguirà tack ServerService ovvero thread che ascolterà le varie richieste dai client.
        Thread thread = new Thread(new ServerService(port,registeredList,usersList,fileJsonName,server,counters));

        thread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                thread.interrupt();

                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Server terminato...");
            System.exit(1);
        }));

        /*  -------- Protocollo di terminazione -------- */
        boolean term = false;
        Scanner sc = new Scanner(System.in);

        while(!term) {

            if (sc.nextLine().equals("termina")){
                // Il server termina se inserisco stringa "termina"
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
        System.exit(1);
        /*  -------------------------------------------- */


    }

    /**
     * Legge da file BackupServer.json e ottiene una stringa
     *
     * @param pat path di BackupServer.json
     *
     *
     * @return result (String JSON)
     * @throws IOException durante la lettura del file
     * */
    public static String readJson(String pat) throws IOException{

        Path path = Paths.get(".");
        Path JsonNioPath = path.resolve(pat);
        StringBuilder result= new StringBuilder();
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
                result.append(tmp);
            }
            byteBufferReader.clear();
        }
        inChannel.close();

        return result.toString();
    }


    /**
     * Aggiunge utenti a tabella hash di Counters
     *
     * @param counters classe Counters usata per gestire contatori delle sfide
     * @param result string contenente il contenuto di BackupServer.json
     */
    public static void buildCounters(Counters counters,String result){
        JSONArray jsonArray;
        JSONParser parser = new JSONParser();

        try{
            jsonArray = (JSONArray) parser.parse(result);
            // ottengo array con tutti gli utenti

            for (Object obj : jsonArray) {
                JSONObject value = (JSONObject) obj;
                String username = (String) value.get("username");

                counters.addUser(username);

            }
        }catch (ParseException e){
            e.printStackTrace();
        }
    }


    /**
     * Costruisce registeredList con i dati che trova sul file di backup in json.
     *
     * @param registeredList TreeMap che conterrà tutti i dati degli utenti
     * @param result stringa ottenuta dalla lettura di BackupServer.json
     */
    public static void buildRegistered(TreeMap<String, Utente> registeredList, String result){

        JSONArray jsonArray;
        JSONParser parser = new JSONParser();

        try{
            jsonArray = (JSONArray) parser.parse(result);
            // ottengo array con tutti gli utenti

            for (Object o : jsonArray) {
                JSONObject obj = (JSONObject) o;
                String username = (String) obj.get("username");
                String password = (String) obj.get("password");
                Long point = (Long) obj.get("points");

                Utente utente = new Utente(username, password, point);
                JSONArray listaF = (JSONArray) obj.get("friends");
                for (Object value : listaF) {
                    JSONObject object = (JSONObject) value;
                    utente.setFriend(object.get("username").toString());
                }
                registeredList.put(utente.getUsername(), utente);

            }
        }catch (ParseException e){
            e.printStackTrace();
        }
    }

}
