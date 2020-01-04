import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;

public class MainClassClient {

    private static int DEFAULT_PORT = 13200;
    private static int PORT_FOR_RSERVICE = 9999;
    private static String host;

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

        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException e) {
            port = DEFAULT_PORT;
        }

        Registry reg = LocateRegistry.getRegistry(host, PORT_FOR_RSERVICE);

        RemoteRegistration registration = (RemoteRegistration) reg.lookup(RemoteRegistration.SERVICE_NAME);

        Registry registry = LocateRegistry.getRegistry(host, 5000);
        ServerInterface server = (ServerInterface) registry.lookup(ServerInterface.SERVICE_NAME);

        NotifyEventInterface callbackObj = new NotifyEventImpl();
        NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
        server.registerForCallback("Michele", stub);

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

        SocketAddress address = new InetSocketAddress(host, port);
        SocketChannel client = SocketChannel.open(address);
        client.configureBlocking(true);

        boolean close = false;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while (!close) {

            String in = input.readLine();
            String[] elenco = in.split(" ");
            try {
                switch (elenco[0]) {
                    case "registra_utente": {
                        String username = elenco[1];
                        String passw = elenco[2];
                        int res = registration.registra(username, passw);

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
                        String username = elenco[1];
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
                        String username = elenco[1];
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
                    break;
                    case "aggiungi_amico": {
                        String username = elenco[1];
                        String friend = elenco[2];

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
                    break;
                    case "lista_amici": {
                        String username = elenco[1];
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
                    break;
                    case "mostra_punteggio": {
                        String username = elenco[1];

                        String toServer = "mostra_punteggio/" + username;
                        ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());
                        client.write(buffer);

                        ByteBuffer fer = ByteBuffer.allocate(1024);
                        client.read(fer);

                        fer.flip();
                        String risposta = "" + StandardCharsets.UTF_8.decode(fer).toString();
                        System.out.println("Punteggio: " + risposta);
                    }
                    break;
                    case "mostra_classifica": {
                        String username = elenco[1];

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
                    break;
                    case "sfida": {
                        String username = elenco[1];
                        String friend = elenco[2];

                        String toServer = "sfida/" + username + "/" + friend;
                        ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());
                        client.write(buffer);

                        ByteBuffer fer = ByteBuffer.allocate(1024);
                        client.read(fer);

                        fer.flip();
                        String risposta = "" + StandardCharsets.UTF_8.decode(fer).toString();
                        System.out.println(risposta);

                        if (risposta.contains("Scrivi")) {
                            String[] lista = risposta.split(":");
                            int newport = Integer.parseInt(lista[1]);
                            System.out.println("nuova porta: " + newport);
                            DatagramSocket clientSocket;
                            InetAddress IPAddress;
                            clientSocket = new DatagramSocket();
                            IPAddress = InetAddress.getByName(host);


                            String rip = "sfidante";

                            byte[] sendData;
                            sendData = rip.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, newport);

                            clientSocket.send(sendPacket);
                            System.out.println(new String(sendData));
                            byte[] receiveData = new byte[1024];

                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, newport);
                            clientSocket.receive(receivePacket);
                            System.out.println("QUI");
                            System.out.println(new String(receiveData));


                            clientSocket.receive(receivePacket);

                            String initsfida = new String(receiveData);

                            String[] el = initsfida.split("/");
                            long timeout = Long.parseLong(el[0]);

                            int countWord = Integer.parseInt(el[1]);

                            String nextWord = el[2];
                            Thread time = new Thread(new Timeout(timeout));
                            time.start();
                            int i = 1;
                            System.out.println("Via alla sfida di traduzione!");
                            System.out.println("Avete " + timeout + " secondi per tradurre correttamente " + countWord + " parole.");
                            while (time.isAlive() && i <= countWord) {
                                BufferedReader insert = new BufferedReader(new InputStreamReader(System.in));
                                System.out.println("Challenge " + i + "/" + countWord + ": " + nextWord);
                                System.out.printf(">");
                                String risp = "parola/" + i + "/" + countWord + "/" + insert.readLine().substring(1);
                                sendData = risp.getBytes();
                                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, newport);
                                clientSocket.send(sendPacket);

                                clientSocket.receive(receivePacket);
                                risp = new String(receiveData);

                                String[] sl = risp.split("/");

                                nextWord = sl[3];
                                i++;

                                insert.close();
                            }

                            clientSocket.receive(receivePacket);

                            System.out.println(new String(receiveData));

                        }
                    }
                    break;
                    case "exit": {
                        close = true;
                        client.close();
                        input.close();

                    }
                    break;

                    default:
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
                }
            }catch (Exception e){
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
        System.exit(1);

    }
    public static void listenUDPreq(int port) throws Exception{

        DatagramSocket clientSocket;
        InetAddress IPAddress;

        clientSocket = new DatagramSocket();
        IPAddress = InetAddress.getByName(host);

        byte[] receiveData = new byte[1024];
        DatagramPacket sendPacket;

        sendPacket = new DatagramPacket("sfidato".getBytes(),"sfidato".length(),IPAddress,port);
        clientSocket.send(sendPacket);
        System.out.println(port);

        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length,IPAddress,port);


        clientSocket.receive(receivePacket);
        System.out.println("PORCI");

        String ricevuta = new String(receiveData);

        String[] elenco = ricevuta.split("/");

        Thread timeout = new Thread(new Timeout(Long.parseLong(elenco[1])));
        timeout.start();
        System.out.println(new String(receiveData));

        System.out.println("1. accetta sfida");
        System.out.println("2. rifiuta sfida");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String in = input.readLine();

        int ok=0;

        if(timeout.isAlive()) {
            switch (in) {
                case "1":
                    sendPacket = new DatagramPacket("ok".getBytes(), "ok".length(), IPAddress, port);
                    clientSocket.send(sendPacket);
                    ok = 1;
                    break;
                case "2":
                    sendPacket = new DatagramPacket("not ok".getBytes(), "ok".length(), IPAddress, port);
                    clientSocket.send(sendPacket);
                    break;

            }
        } else {
            sendPacket = new DatagramPacket("tempo scaduto per accettare".getBytes(), "tempo scaduto per accettare".length(), IPAddress, port);
            clientSocket.send(sendPacket);
        }

        if(ok == 1){
            clientSocket.receive(receivePacket);

            String initsfida = new String(receiveData);

            String[] el = initsfida.split("/");
            long timeoutt = Long.parseLong(el[0]);

            int countWord = Integer.parseInt(el[1]);

            String nextWord = el[2];
            Thread time = new Thread(new Timeout(timeoutt));
            time.start();
            int i =1;
            while(time.isAlive() && i<=countWord){
                BufferedReader in2 = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Challenge " + i + "/" + countWord + ": " + nextWord);
                String risp = "parola/"+i+"/"+countWord+"/"+in2.readLine();
                sendPacket = new DatagramPacket(risp.getBytes(),risp.length(),IPAddress,port);
                clientSocket.send(sendPacket);

                clientSocket.receive(receivePacket);
                risp = new String(receiveData);

                String[] sl = risp.split("/");

                nextWord = sl[3];
                i++;
                in2.close();

            }

            clientSocket.receive(receivePacket);

            System.out.println(new String(receiveData));

        }
    }
}
