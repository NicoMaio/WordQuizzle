import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainClassClient {

    private static int DEFAULT_PORT = 13200;
    private static int PORT_FOR_RSERVICE = 9999;
    private static String host;
    private static AtomicBoolean close;
    final static BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    public static AtomicBoolean sfidato;
    public static int port;
    public static Thread t;
    private static Lock lock;
    private static DatagramSocket clientSocket;
    private static InetAddress IPAddress;
    private static Thread timeout;


    private static JTextField xInput,yInput;
    public static void main(String[] args) throws Exception {
        // MainClassClient host port
        // host: nome del host Server di WordQuizzle.
        // port: numero di porta su cui è in attesa il server.

        if (args.length == 0) {
            System.err.println("Usage: java MainClassClient host port");
            return;
        }

        host = args[0];


        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException e) {
            port = DEFAULT_PORT;
        }

        lock = new ReentrantLock() ;
        Registry reg = LocateRegistry.getRegistry(host, PORT_FOR_RSERVICE);

        RemoteRegistration registration = (RemoteRegistration) reg.lookup(RemoteRegistration.SERVICE_NAME);

        Registry registry = LocateRegistry.getRegistry(host, 5000);
        ServerInterface server = (ServerInterface) registry.lookup(ServerInterface.SERVICE_NAME);

        NotifyEventInterface callbackObj = new NotifyEventImpl();
        NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
        server.registerForCallback("Michele", stub);
        close = new AtomicBoolean();
        close.set(false);
        sfidato = new AtomicBoolean();
        listenCommand(port,registration,server,stub);

        /*
        SocketAddress address = new InetSocketAddress(host,port);
        SocketChannel client = SocketChannel.open(address);
        client.configureBlocking(true);

        Operation op = new Operation(registration,server,client);
        MainForm form = new MainForm(op);

        JFrame window = new JFrame("GUI Test");
        JPanel main = new JPanel();
        //window.getContentPane().add(main);

        JPanel username = new JPanel();
        JPanel password = new JPanel();
        xInput = new JTextField("",10);

        yInput = new JTextField("",10);
        xInput.setBackground(Color.WHITE);
        yInput.setBackground(Color.WHITE);
        username.add(new JLabel("username"));
        username.add(xInput);

        password.add(new JLabel("password"));
        password.add(yInput);

        JButton invio = new JButton("invio");
        ActionListener ok = new RunList(xInput,yInput);
        invio.addActionListener(ok);

        password.add(invio);


        window.getContentPane().add(main);
        JButton button = new JButton("sign up");
        main.add(button);
        ActionListener listener = new ClickListener(main,username,password,window);
        button.addActionListener(listener);
        window.setSize(800,600);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLocation(100,100);
        window.setVisible(true);

        */


        // stabilisco connessione con server.
        // configuro connessione bloccante lato server.


        System.exit(1);

    }
    public static void listenCommand(int nport,RemoteRegistration registration,ServerInterface server,NotifyEventInterface stub)throws Exception{
        SocketAddress address = new InetSocketAddress(host, nport);
        SocketChannel client = SocketChannel.open(address);
        client.configureBlocking(true);

        String username = null;
        while (!close.get()) {

            String in = null;
            if(sfidato.get()){

                t.join();
                sfidato.set(false);
            }
            if (!close.get() && input != null) {
                if (sfidato.get()) {

                    t.join();
                    sfidato.set(false);
                }
                //System.out.println("OOOOSSS");
                System.out.printf(">");
                lock.lock();
                    in = input.readLine();
                lock.unlock();
            }
            if (in != null) {
                String[] elenco = in.split(" ");
                try {
                    switch (elenco[0]) {
                        case "registra_utente": {
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
                            username = elenco[1];
                            String password = elenco[2];

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
                                    server.registerForCallback(username, stub);
                                    break;
                            }

                        }
                        break;
                        case "logout": {
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

                                    Iterator iterator = jsonArray.iterator();
                                    String result = "";
                                    while (iterator.hasNext()) {
                                        JSONObject obj = (JSONObject) iterator.next();
                                        String utente = (String) obj.get("username");
                                        result += utente;
                                        if (iterator.hasNext()) {
                                            result += ", ";
                                        }
                                    }

                                    System.out.println(result);

                                } catch (ParseException e) {
                                }
                            }
                        }
                        break;
                        case "mostra_punteggio": {

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
                                    String result = "Classifica: ";
                                    while (iterator.hasNext()) {
                                        JSONObject obj = (JSONObject) iterator.next();
                                        String utente = (String) obj.get("username");
                                        Long punteggio = (Long) obj.get("points");
                                        result += utente + " " + punteggio;
                                        if (iterator.hasNext()) {
                                            result += ", ";
                                        }
                                    }

                                    System.out.println(result);

                                } catch (ParseException e) {
                                }
                            }
                        }
                        break;
                        case "sfida": {
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
                                        int i = 0;
                                        System.out.println("Via alla sfida di traduzione!");
                                        System.out.println("Avete " + timeout + " secondi per tradurre correttamente " + countWord + " parole.");

                                        boolean stayHere = false;
                                        while (time.isAlive() && !stayHere) {
                                            System.out.println("Challenge " + i + "/" + (countWord - 1) + ": " + nextWord);
                                            System.out.printf(">");
                                            String word = input.readLine();
                                            i = i + 1;
                                            String risp = "parola/" + (i) + "/" + countWord + "/" + word;
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
                            close.set(true);
                            client.close();
                            input.close();

                        }
                        break;

                        case "wq": {
                            System.out.println("usage : COMMANDS [ ARGS ...]");
                            System.out.println("Commands:");
                            System.out.println(" registra_utente <nickUtente> <password>");  // registra l' utente
                            System.out.println(" login <nickUtente> <password>");  //effettua il login
                            System.out.println(" logout <nickUtente>");  // effettua il logout
                            System.out.println(" aggiungi_amico <nickAmico>");   // crea relazione di amicizia con nickAmico
                            System.out.println(" lista_amici <nickUtente>");  //mostra la lista dei propri amici
                            System.out.println(" sfida <nickUtente> <nickAmico>");  //richiesta di una sfida a nickAmico
                            System.out.println(" mostra_punteggio <nickUtente>");  //mostra il punteggio dell’utente
                            System.out.println(" mostra_classifica <nickUtente>");   //mostra una classifica degli amici dell’utente (incluso l’utente stesso)
                            System.out.println("exit"); // termina il client
                        }
                        break;
                        case "1": {
                            DatagramPacket sendPacket;

                            if (timeout.isAlive()) {

                                sendPacket = new DatagramPacket("ok".getBytes(), "ok".length(), IPAddress, port);
                                try {
                                    clientSocket.send(sendPacket);
                                    //input.close();
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
                                    int i = 0;
                                    boolean stayHere = false;
                                    try {
                                        while (time.isAlive() && !stayHere) {
                                            System.out.println("Challenge " + i + "/" + (countWord - 1) + ": " + nextWord);
                                            System.out.printf(">");
                                            String word = input.readLine();
                                            //System.out.println(word);
                                            i = i + 1;
                                            String risp = "parola/" + (i) + "/" + countWord + "/" + word;
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

                                                    //System.out.println(risp);
                                                    nextWord = sl[3];
                                                } else {
                                                    stayHere = true;
                                                    System.out.println(risp);

                                                }
                                                //System.out.println("QI");
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
                                    //input.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                System.out.println("Tempo scaduto per accettare");
                            }


                        }
                        break;
                        case "2":{
                            DatagramPacket sendPacket;
                            if(timeout.isAlive()){
                            sendPacket = new DatagramPacket("not ok".getBytes(), "not ok".length(), IPAddress, port);
                            try {
                                clientSocket.send(sendPacket);
                                //input.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            } else {
                                sendPacket = new DatagramPacket("tempo scaduto per accettare".getBytes(), "tempo scaduto per accettare".length(), IPAddress, port);
                                try {
                                    clientSocket.send(sendPacket);
                                    //input.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                System.out.println("Tempo scaduto per accettare");
                            }
                        }
                        break;
                        default: {
                            System.out.println(elenco[0]);
                            System.out.println("usage : COMMANDS [ ARGS ...]");
                            System.out.println("Commands:");
                            System.out.println(" registra_utente <nickUtente> <password>");  // registra l' utente
                            System.out.println(" login <nickUtente> <password>");  //effettua il login
                            System.out.println(" logout <nickUtente>");  // effettua il logout
                            System.out.println(" aggiungi_amico <nickAmico>");   // crea relazione di amicizia con nickAmico
                            System.out.println(" lista_amici <nickUtente>");  //mostra la lista dei propri amici
                            System.out.println(" sfida <nickUtente> <nickAmico>");  //richiesta di una sfida a nickAmico
                            System.out.println(" mostra_punteggio <nickUtente>");  //mostra il punteggio dell’utente
                            System.out.println(" mostra_classifica <nickUtente>");   //mostra una classifica degli amici dell’utente (incluso l’utente stesso)
                            System.out.println("exit"); // termina il client
                            break;

                        }

                    }
                }catch (Exception e) {
                    System.out.println("usage : COMMANDS [ ARGS ...]");
                    System.out.println("Commands:");
                    System.out.println(" registra_utente <nickUtente> <password>");  // registra l' utente
                    System.out.println(" login <nickUtente> <password>");  //effettua il login
                    System.out.println(" logout <nickUtente>");  // effettua il logout
                    System.out.println(" aggiungi_amico <nickAmico>");   // crea relazione di amicizia con nickAmico
                    System.out.println(" lista_amici <nickUtente>");  //mostra la lista dei propri amici
                    System.out.println(" sfida <nickUtente> <nickAmico>");  //richiesta di una sfida a nickAmico
                    System.out.println(" mostra_punteggio <nickUtente>");  //mostra il punteggio dell’utente
                    System.out.println(" mostra_classifica <nickUtente>");   //mostra una classifica degli amici dell’utente (incluso l’utente stesso)
                    System.out.println("exit"); // termina il client

                }

            }
        }
        input.close();
    }
    public static void runExec(int nport){
        //System.out.println("OOOK");
        port = nport;
        sfidato.set(true);
        t = new Thread( new Exec(port));

        t.start();
    }

    public static void listenUDPreq(int port){

        //lock.lock();

            //System.out.println("OOOOO");



            try {
                clientSocket = new DatagramSocket();
                IPAddress = InetAddress.getByName(host);
            } catch (IOException e) {

            }
            byte[] receiveData = new byte[1024];
            DatagramPacket sendPacket;

            String sfida = "sfidato";
            sendPacket = new DatagramPacket(sfida.getBytes(), sfida.length(), IPAddress, port);
            try {
                clientSocket.send(sendPacket);
            } catch (IOException e) {

            }
            //System.out.println(port);

            //System.out.println(new String(sfida.getBytes()));
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, port);


            try {
                clientSocket.receive(receivePacket);
            } catch (IOException e) {

            }
            //System.out.println("PORCI");

            String ricevuta = new String(receiveData);

            //System.out.println(ricevuta);
            String[] elenco = ricevuta.split("/");
            int tempo = 10000;

            timeout = new Thread(new Timeout(tempo));
            timeout.start();
            //System.out.println("QUI");

            System.out.println(elenco[0]);

            System.out.println("1. accetta sfida");
            System.out.println("2. rifiuta sfida");


    }
}
