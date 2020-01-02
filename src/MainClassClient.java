import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MainClassClient {

    private static int DEFAULT_PORT = 13200;
    private static int PORT_FOR_RSERVICE = 9999;
    private static String host;
    public static void main(String[] args) throws Exception{
        // MainClassClient host port
        // host: nome del host Server di WordQuizzle.
        // port: numero di porta su cui è in attesa il server.

        if(args.length == 0){
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

        Registry reg = LocateRegistry.getRegistry(host,PORT_FOR_RSERVICE);

        RemoteRegistration registration = (RemoteRegistration) reg.lookup(RemoteRegistration.SERVICE_NAME);

        int x =registration.registra("Michele","Superman");

        registration.registra("Francesco","Illegale");

        if(x == -1)System.out.println("Utente Michele già registrato");


        Registry registry = LocateRegistry.getRegistry(host,5000);
        ServerInterface server = (ServerInterface) registry.lookup(ServerInterface.SERVICE_NAME);

        NotifyEventInterface callbackObj = new NotifyEventImpl();
        NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj,0);
        server.registerForCallback("Michele",stub);



        // stabilisco connessione con server.
        // configuro connessione bloccante lato server.

        SocketAddress address = new InetSocketAddress(host,port);
        SocketChannel client = SocketChannel.open(address);
        client.configureBlocking(true);

       // while (true){

        String input = "login/Michele/Superman";

        ByteBuffer buffer = ByteBuffer.wrap(input.getBytes());

        client.write(buffer);
        buffer.clear();
        buffer.flip();

        String response="";


        ByteBuffer fer = ByteBuffer.allocate(1024);

        client.read(fer);
        fer.flip();

        response +=StandardCharsets.UTF_8.decode(fer).toString();



        System.out.println(response);

        switch (response){
            case "-2":
                System.out.println("Utente non è registrato");
                break;
            case "-1":
                System.out.println("Password errata");

                break;
            case "0":
                System.out.println("Login già effettuato");

                break;
            case "1":
                System.out.println("Login ok");

                break;
        }

         input = "login/Francesco/Illegale";

        buffer = ByteBuffer.wrap(input.getBytes());

        client.write(buffer);
        buffer.clear();
        buffer.flip();

         response="";


         fer = ByteBuffer.allocate(1024);

        client.read(fer);
        fer.flip();

        response +=StandardCharsets.UTF_8.decode(fer).toString();



        System.out.println(response);

        switch (response){
            case "-2":
                System.out.println("Utente non è registrato");
                break;
            case "-1":
                System.out.println("Password errata");

                break;
            case "0":
                System.out.println("Login già effettuato");

                break;
            case "1":
                System.out.println("Login ok");

                break;
        }

        String input3 = "lista_amici/Michele";
        buffer = ByteBuffer.wrap(input3.getBytes());
        client.write(buffer);

        fer = ByteBuffer.allocate(1024);
        client.read(fer);

        fer.flip();
        String risposta = ""+StandardCharsets.UTF_8.decode(fer).toString();

        System.out.println(risposta);

        input3= "sfida/Michele/Francesco";
        buffer = ByteBuffer.wrap(input3.getBytes());
        client.write(buffer);

        fer = ByteBuffer.allocate(1024);
        client.read(fer);

        fer.flip();
        risposta = ""+StandardCharsets.UTF_8.decode(fer).toString();
        System.out.println(risposta);

        if(risposta.contains("Scrivi")){
            String[] lista = risposta.split(":");
            int newport = Integer.parseInt(lista[1]);
            System.out.println("nuova porta: "+newport);
            DatagramSocket clientSocket;
            InetAddress IPAddress;
            clientSocket = new DatagramSocket();
            IPAddress = InetAddress.getByName(host);


            String rip = "sfidante";

            byte[] sendData;
            sendData=rip.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddress,newport);

            clientSocket.send(sendPacket);
            System.out.println(new String(sendData));
            byte[] receiveData = new byte[1024];

            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length,IPAddress,newport);
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
            int i =1;
            while(time.isAlive() && i<=countWord){
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                System.out.println(i+"/"+countWord+" : "+nextWord);
                System.out.println("Per lasciare vuota manda spazio");
                String risp = "parola/"+i+"/"+countWord+"/"+in.readLine();
                sendData=risp.getBytes();
                sendPacket = new DatagramPacket(sendData,sendData.length,IPAddress,newport);
                clientSocket.send(sendPacket);

                clientSocket.receive(receivePacket);
                risp = new String(receiveData);

                String[] sl = risp.split("/");

                nextWord = sl[3];
                i++;

                in.close();
            }

            clientSocket.receive(receivePacket);

            System.out.println(new String(receiveData));




        }


        input3 = "mostra_punteggio/Michele";
        buffer = ByteBuffer.wrap(input3.getBytes());
        client.write(buffer);

        fer = ByteBuffer.allocate(1024);
        client.read(fer);

        fer.flip();
        risposta = ""+StandardCharsets.UTF_8.decode(fer).toString();
        System.out.println(risposta);


        input3 = "mostra_classifica/Michele";
        buffer = ByteBuffer.wrap(input3.getBytes());
        client.write(buffer);

        fer = ByteBuffer.allocate(1024);
        client.read(fer);

        fer.flip();
        risposta = ""+StandardCharsets.UTF_8.decode(fer).toString();

        System.out.println(risposta);





        String input2 = "logout/Michele";

        ByteBuffer buffer2 = ByteBuffer.wrap(input2.getBytes());

        client.write(buffer2);

        ByteBuffer fer2 = ByteBuffer.allocate(1024);
        client.read(fer2);
        fer2.flip();
        String resp =""+ StandardCharsets.UTF_8.decode(fer2).toString();

        switch(resp){
            case "1":
                System.out.println("logout ok");
                break;
            case "-1":
                System.out.println("logout not ok");
                break;
        }

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
        DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length,IPAddress,port);


        clientSocket.receive(receivePacket);
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
                System.out.println(i+"/"+countWord+" : "+nextWord);
                System.out.println("Per lasciare vuota manda spazio");
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
