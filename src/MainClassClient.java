/**
 * @author Nicolò Maio
 *
 * MainClassClientr: Classe principale del Client di WordQuizzle.
 *
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
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainClassClient {

    private static String host;
    // stringa contenente nome dell'host del server.

    private static AtomicBoolean close;
    // atomicBoolean usata per gestire terminazione del server.

    final static BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    // bufferedReader usato per ricevere in input richieste dall'utente.

    public static AtomicBoolean sfidato;
    // atomicBoolean usata per capire se l'utente è sotto sfida.

    public static int port;
    // numero di porta al quale dovrà inviare le richieste il client.

    public static Thread t;
    // thread che eseguirà al momento della richiesta ad una sfida il modulo per accettare o rifiutare richiesta di sfida.

    private static DatagramSocket clientSocket;
    // datagramSocket sul quale il client riceverà richiesta UDP in caso di sfida

    private static InetAddress IPAddress;
    // IP address del server usato per i messaggi UDP.

    private static Thread timeout;
    // thread Timeout usato per gestire i timeout della sfida.

    private static  String username;

    public static void main(String[] args) throws Exception {
        // MainClassClient host port
        // host: nome del host Server di WordQuizzle.
        // port: numero di porta su cui è in attesa il server.

        /* ---------------- Sistemo hostname e porta ---------------- */
        if (args.length == 0) {
            System.err.println("Usage: java MainClassClient host port");
            return;
        }

        host = args[0];

        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException e) {
            port = 13200;
        }
        /* ---------------------------------------------------------- */

        /* ---------------- Imposto servizio RMI per operazione di registrazione ---------------- */
        int PORT_FOR_RSERVICE = 9999;
        Registry reg = LocateRegistry.getRegistry(host, PORT_FOR_RSERVICE);

        RemoteRegistration registration = (RemoteRegistration) reg.lookup(RemoteRegistration.SERVICE_NAME);
        /* -------------------------------------------------------------------------------------- */

        /* ---------------- Imposto servizio RMI per operazione di callback in caso di richiesta per sfida ---------------- */
        Registry registry = LocateRegistry.getRegistry(host, 5000);
        ServerInterface server = (ServerInterface) registry.lookup(ServerInterface.SERVICE_NAME);

        NotifyEventInterface callbackObj = new NotifyEventImpl();
        NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
        /* ---------------------------------------------------------------------------------------------------------------- */

        // Inizializzo AtomicBoleaan
        close = new AtomicBoolean();
        sfidato = new AtomicBoolean();

        // avvio modulo per ascoltare richieste
        listenCommand(port,registration,server,stub);
        System.exit(1);

    }

    /**
     * @param nport porta del server alla quale mandare richieste TCP.
     * @param registration istanza di ImplRemoteRegistration per gestire operazione di registrazione.
     * @param server istanza di ServerCallBImpl per registrare utente e gestire richieste di sfida.
     * @param stub stub delle richieste di sfida gestite con RMI callback.
     * @throws Exception varie eccezioni che possono essere lanciate in caso di vari errori.
     */
    public static void listenCommand(int nport,RemoteRegistration registration,ServerInterface server,NotifyEventInterface stub)throws Exception{

        /* ------- Inizializzo address e socketChannel per richieste TCP ------- */
        SocketAddress address = new InetSocketAddress(host, nport);
        SocketChannel client = SocketChannel.open(address);
        client.configureBlocking(true);
        /* --------------------------------------------------------------------- */




        // loop per gestire le varie richieste
        while (!close.get()) {

            String in = null;
            if(sfidato.get()){

                t.join();
                sfidato.set(false);
            }
            if (!close.get()) {
                if (sfidato.get()) {

                    t.join();
                    sfidato.set(false);
                }
                System.out.print(">");
                // prendo in input la richiesta
                in = input.readLine();
            }
            if (in != null) {
                String[] elenco = in.split(" ");
                try {

                    // switcho prima stringa della richiesta
                    switch (elenco[0]) {
                        case "registra_utente": {

                            // gestisco operazione di registrazione dell'utente
                            String user = elenco[1];
                            String passw = elenco[2];
                            int res = registration.registra(user, passw);

                            switch (res) {
                                case 0: {
                                    System.out.println("Password inserita scorrettamente");
                                }
                                break;
                                case -1: {
                                    System.out.println("Username scelto è già presente");
                                }
                                break;
                                case 1: {
                                    System.out.println("Registrazione eseguita con successo.");

                                }
                                break;
                            }
                        }
                        break;

                        case "login": {
                            // gestisco operazione di login
                            if(username != null){
                                System.out.println("Un altro utente si è già loggato in questa sessione");
                            } else {


                                username = elenco[1];
                                String password = elenco[2];

                                Runtime.getRuntime().addShutdownHook(new Thread(new ShutDownerThread(username, client)));


                                String toServer = "login/" + username + "/" + password;

                                ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());

                                client.write(buffer);
                                buffer.clear();
                                buffer.flip();

                                String response = "";


                                ByteBuffer fer = ByteBuffer.allocate(1024);

                                client.read(fer);
                                fer.flip();

                                response += StandardCharsets.UTF_8.decode(fer).toString();

                                switch (response) {
                                    case "-2":
                                        System.out.println("Username errato");
                                        break;
                                    case "-1":
                                        System.out.println("Password errata");
                                        break;
                                    case "0":
                                        System.out.println("Login già effettuato");
                                        break;
                                    case "1":
                                        System.out.println("Login eseguito con successo");
                                        // registro l'utente per ricevere notifiche in callback in caso di richiesta di sfida
                                        server.registerForCallback(username, stub);
                                        break;
                                }
                            }
                        }
                        break;
                        case "logout": {

                            // gestisco operazione di logout
                            if(username== null){
                                System.out.println("Need a login before");
                            } else {
                                String toServer = "logout/" + username;

                                ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());

                                client.write(buffer);

                                ByteBuffer fer2 = ByteBuffer.allocate(1024);
                                client.read(fer2);
                                fer2.flip();
                                String resp = "" + StandardCharsets.UTF_8.decode(fer2).toString();

                                switch (resp) {
                                    case "1":
                                        System.out.println("Logout avvenuto con successo");
                                        break;
                                    case "-1":
                                        System.out.println("Logout fallito");
                                        break;
                                }
                            }

                        }
                        break;

                        case "aggiungi_amico": {

                            // gestisco operazione di aggiunta amico
                            if(username== null){
                                System.out.println("Need a login before");
                            } else {
                                String friend = elenco[1];

                                String toServer = "aggiungi_amico/" + username + "/" + friend;

                                ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());

                                client.write(buffer);

                                ByteBuffer fer2 = ByteBuffer.allocate(1024);
                                client.read(fer2);
                                fer2.flip();
                                String resp = "" + StandardCharsets.UTF_8.decode(fer2).toString();

                                switch (resp) {
                                    case "-1": {
                                        System.out.println("Username dell'amico indicato non è registrato");
                                    }
                                    break;
                                    case "-2": {
                                        System.out.println(friend + " è già tuo amico");
                                    }
                                    break;
                                    case "1": {
                                        System.out.println("Amicizia " + username + "-" + friend + " creata.");
                                    }
                                }
                            }
                        }
                        break;
                        case "lista_amici": {

                            // gestisco operazione di richiesta lista amici
                            if(username== null){
                                System.out.println("Need a login before");
                            } else {
                                String toServer = "lista_amici/" + username;

                                ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());
                                client.write(buffer);

                                ByteBuffer fer = ByteBuffer.allocate(1024);
                                client.read(fer);

                                fer.flip();
                                String risposta = "" + StandardCharsets.UTF_8.decode(fer).toString();

                                JSONArray jsonArray;
                                JSONParser parser = new JSONParser();

                                try {
                                    jsonArray = (JSONArray) parser.parse(risposta);

                                    Iterator<JSONObject> iterator = jsonArray.iterator();
                                    StringBuilder result = new StringBuilder();
                                    while (iterator.hasNext()) {
                                        JSONObject obj = iterator.next();
                                        String utente = (String) obj.get("username");
                                        result.append(utente);
                                        if (iterator.hasNext()) {
                                            result.append(", ");
                                        }
                                    }

                                    System.out.println(result);

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;
                        case "mostra_punteggio": {

                            // gestisco operazione di richiesta mostra punteggio dell'utente appena loggato
                            if(username== null){
                                System.out.println("Need a login before");
                            } else {
                                String toServer = "mostra_punteggio/" + username;
                                ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());
                                client.write(buffer);

                                ByteBuffer fer = ByteBuffer.allocate(1024);
                                client.read(fer);

                                fer.flip();
                                String risposta = "" + StandardCharsets.UTF_8.decode(fer).toString();
                                System.out.println("Punteggio: " + risposta);
                            }
                        }
                        break;

                        case "mostra_classifica": {

                            // gestisco operazione di mostra classifica dell'utente loggato e dei suoi amici.
                            if(username== null){
                                System.out.println("Need a login before");
                            } else {
                                String toServer = "mostra_classifica/" + username;
                                ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());
                                client.write(buffer);

                                ByteBuffer fer = ByteBuffer.allocate(1024);
                                client.read(fer);

                                fer.flip();
                                String risposta = "" + StandardCharsets.UTF_8.decode(fer).toString();

                                JSONArray jsonArray;
                                JSONParser parser = new JSONParser();

                                try {
                                    jsonArray = (JSONArray) parser.parse(risposta);

                                    Iterator iterator = jsonArray.iterator();
                                    StringBuilder result = new StringBuilder("Classifica: ");
                                    while (iterator.hasNext()) {
                                        JSONObject obj = (JSONObject) iterator.next();
                                        String utente = (String) obj.get("username");
                                        Long punteggio = (Long) obj.get("points");
                                        result.append(utente).append(" ").append(punteggio);
                                        if (iterator.hasNext()) {
                                            result.append(", ");
                                        }
                                    }

                                    System.out.println(result);

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        break;

                        case "sfida": {

                            // gestisco operazione di invio richiesta di sfida
                            if (username == null) {
                                System.out.println("Need a login before");
                            } else {
                                String friend = elenco[1];

                                String toServer = "sfida/" + username + "/" + friend;
                                ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());
                                client.write(buffer);

                                ByteBuffer fer = ByteBuffer.allocate(1024);
                                client.read(fer);

                                fer.flip();
                                String risposta = "" + StandardCharsets.UTF_8.decode(fer).toString();
                                //System.out.println(risposta);

                                if (risposta.contains("Scrivi")) {
                                    String[] lista = risposta.split(":");
                                    int newport = Integer.parseInt(lista[1]);
                                    //System.out.println("nuova porta: " + newport);
                                    DatagramSocket clientSocket;
                                    InetAddress IPAddress;
                                    clientSocket = new DatagramSocket();
                                    IPAddress = InetAddress.getByName(host);


                                    String rip = "sfidante";

                                    byte[] sendData;
                                    sendData = rip.getBytes();
                                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, newport);

                                    clientSocket.send(sendPacket);
                                    //System.out.println(new String(sendData));
                                    byte[] receiveData = new byte[1024];

                                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, newport);
                                    clientSocket.receive(receivePacket);
                                    System.out.println(new String(receiveData));


                                    ByteBuffer reader = ByteBuffer.allocate(1024);
                                    client.read(reader);
                                    reader.flip();
                                    String initsfida = StandardCharsets.UTF_8.decode(reader).toString();
                                    if (initsfida.contains("60000")) {

                                        String[] el = initsfida.split("/");
                                        int timeout = Integer.parseInt(el[0]);

                                        int countWord = Integer.parseInt(el[1]);

                                        String nextWord = el[2];
                                        Thread time = new Thread(new Timeout(timeout));
                                        time.start();
                                        int i = 1;
                                        System.out.println("Via alla sfida di traduzione!");
                                        System.out.println("Avete 60 secondi per tradurre correttamente " + countWord + " parole.");
                                        System.out.println("Per lasciare vuota una risposta invia uno spazio...");


                                        boolean stayHere = false;

                                        while (time.isAlive() && !stayHere) {
                                            System.out.println("Challenge " + i + "/" + (countWord) + ": " + nextWord);
                                            System.out.print(">");
                                            String word = input.readLine();
                                            String risp = "parola/" + (i) + "/" + countWord + "/" + word;
                                            i = i + 1;
                                            reader = ByteBuffer.wrap(risp.getBytes());
                                            if (!time.isAlive()) {
                                                risp = "Tempo Scaduto";
                                                reader = ByteBuffer.wrap(risp.getBytes());
                                                client.write(reader);
                                                reader = ByteBuffer.allocate(1024);
                                                client.read(reader);
                                                reader.flip();
                                                risp = StandardCharsets.UTF_8.decode(reader).toString();
                                                System.out.println("Tempo Scaduto!");
                                                System.out.println(risp);
                                            } else {


                                                client.write(reader);

                                                reader = ByteBuffer.allocate(1024);
                                                client.read(reader);
                                                reader.flip();
                                                risp = StandardCharsets.UTF_8.decode(reader).toString();

                                                if (risp.contains("parola")) {
                                                    String[] sl = risp.split("/");

                                                    nextWord = sl[3];
                                                } else {
                                                    stayHere = true;
                                                    System.out.println(risp);
                                                }
                                            }
                                        }


                                    } else {
                                        System.out.println(initsfida);
                                    }

                                } else {
                                    System.out.println(risposta);
                                }
                            }
                        }
                        break;

                        case "exit": {

                            // gestisco richiesta terminazione client
                            close.set(true);
                            client.close();
                            input.close();

                        }
                        break;

                        case "wq": {

                            // gestisco richiesta di help inviata dall'utente
                            helper2();
                        }
                        break;
                        case "1": {

                            // gestisco caso di sfida accettata dall'utente sfidato
                            if(sfidato.get()) {

                                DatagramPacket sendPacket;

                                if (timeout.isAlive()) {

                                    sendPacket = new DatagramPacket("ok".getBytes(), "ok".length(), IPAddress, port);
                                    try {
                                        clientSocket.send(sendPacket);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    ByteBuffer reader = ByteBuffer.allocate(1024);
                                    client.configureBlocking(true);
                                    client.read(reader);
                                    reader.flip();
                                    String fina = StandardCharsets.UTF_8.decode(reader).toString();
                                    //System.out.println(fina);
                                    if (fina.contains("parola") || fina.contains("60000")) {
                                        String[] el = fina.split("/");
                                        int timeoutt = Integer.parseInt(el[0]);

                                        int countWord = Integer.parseInt(el[1]);

                                        String nextWord = el[2];
                                        Thread time = new Thread(new Timeout(timeoutt));
                                        time.start();
                                        int i = 1;
                                        boolean stayHere = false;
                                        try {
                                            System.out.println("Via alla sfida di traduzione!");
                                            System.out.println("Avete 60 secondi per tradurre correttamente " + countWord + " parole.");
                                            System.out.println("Per lasciare vuota una risposta invia uno spazio...");

                                            while (time.isAlive() && !stayHere) {
                                                System.out.println("Challenge " + i + "/" + (countWord) + ": " + nextWord);
                                                System.out.print(">");
                                                String word = input.readLine();
                                                String risp = "parola/" + (i) + "/" + countWord + "/" + word;
                                                i = i + 1;
                                                reader = ByteBuffer.wrap(risp.getBytes());
                                                if (!time.isAlive()) {
                                                    risp = "Tempo Scaduto";
                                                    reader = ByteBuffer.wrap(risp.getBytes());
                                                    client.write(reader);
                                                    reader = ByteBuffer.allocate(1024);
                                                    client.read(reader);
                                                    reader.flip();
                                                    risp = StandardCharsets.UTF_8.decode(reader).toString();
                                                    System.out.println("Tempo Scaduto!");
                                                    System.out.println(risp);
                                                } else {


                                                    client.write(reader);

                                                    reader = ByteBuffer.allocate(1024);
                                                    client.read(reader);
                                                    reader.flip();
                                                    risp = StandardCharsets.UTF_8.decode(reader).toString();

                                                    if (risp.contains("parola")) {
                                                        String[] sl = risp.split("/");

                                                        nextWord = sl[3];
                                                    } else {
                                                        stayHere = true;
                                                        System.out.println(risp);

                                                    }
                                                }
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    sendPacket = new DatagramPacket("tempo scaduto per accettare".getBytes(), "tempo scaduto per accettare".length(), IPAddress, port);
                                    try {
                                        clientSocket.send(sendPacket);

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Tempo scaduto per accettare");
                                }

                            } else {
                                helper(elenco);
                            }
                        }
                        break;
                        case "2":{

                            // gestisco caso di sfida rifiutata dall'utente sfidato
                            if(sfidato.get()) {
                                DatagramPacket sendPacket;
                                if (timeout.isAlive()) {
                                    sendPacket = new DatagramPacket("not ok".getBytes(), "not ok".length(), IPAddress, port);
                                    try {
                                        clientSocket.send(sendPacket);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }


                                } else {
                                    sendPacket = new DatagramPacket("tempo scaduto per accettare".getBytes(), "tempo scaduto per accettare".length(), IPAddress, port);
                                    try {
                                        clientSocket.send(sendPacket);

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Tempo scaduto per accettare");
                                }
                            } else {
                                helper(elenco);
                            }
                        }
                        break;
                        default: {
                            helper(elenco);
                            break;

                        }

                    }
                }catch (Exception e) {

                    // in caso di eccezione stampo l'help
                    helper2();

                }

            }
        }
        input.close();
    }

    /**
     * Metodo per inviare info sui comandi
     */
    private static void helper2() {
        System.out.println("usage : COMMANDS [ ARGS ...]");
        System.out.println("Commands:");
        System.out.println(" registra_utente <nickUtente> <password>");  // registra l' utente
        System.out.println(" login <nickUtente> <password>");  //effettua il login
        System.out.println(" logout");  // effettua il logout
        System.out.println(" aggiungi_amico <nickAmico>");   // crea relazione di amicizia con nickAmico
        System.out.println(" lista_amici");  //mostra la lista dei propri amici
        System.out.println(" sfida <nickUtente> <nickAmico>");  //richiesta di una sfida a nickAmico
        System.out.println(" mostra_punteggio");  //mostra il punteggio dell’utente
        System.out.println(" mostra_classifica");   //mostra una classifica degli amici dell’utente (incluso l’utente stesso)
        System.out.println("exit"); // termina il client
    }

    /**
     * @param elenco lista di stringa presa in input
     */
    private static void helper(String[] elenco) {
        System.out.println(elenco[0]);
        helper2();
    }

    /**
     * Thread per gestire richiesta di sfida.
     *
     * @param nport porta per richieste UDP
     */
    public static void runExec(int nport){
        port = nport;
        sfidato.set(true);
        t = new Thread( new Exec(port));

        t.start();
    }

    /**
     * Metodo eseguito dal thread che gestisce richiesta di sfida.
     *
     * @param port porta per richieste UDP
     *
     */
    public static void listenUDPreq(int port){

        try {
            clientSocket = new DatagramSocket();
            IPAddress = InetAddress.getByName(host);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] receiveData = new byte[1024];
        DatagramPacket sendPacket;

        String sfida = "sfidato";
        sendPacket = new DatagramPacket(sfida.getBytes(), sfida.length(), IPAddress, port);
        try {
            clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, port);


        try {
            clientSocket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String ricevuta = new String(receiveData);

        String[] elenco = ricevuta.split("/");
        int tempo = 30000;

        timeout = new Thread(new Timeout(tempo));
        timeout.start();

        System.out.println(elenco[0]);
        System.out.println("Hai 30 secondi per accettare la sfida");
        System.out.println("1. accetta sfida");
        System.out.println("2. rifiuta sfida");

    }
}
