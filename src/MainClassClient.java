import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainClassClient {

    private static int DEFAULT_PORT = 13200;
    private static int PORT_FOR_RSERVICE = 9999;
    public static void main(String[] args) throws Exception{
        // MainClassClient host port
        // host: nome del host Server di WordQuizzle.
        // port: numero di porta su cui è in attesa il server.

        if(args.length == 0){
            System.err.println("Usage: java MainClassClient host port");
            return;
        }

        String host = args[0];

        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException e) {
            port = DEFAULT_PORT;
        }

        Registry reg = LocateRegistry.getRegistry(host,PORT_FOR_RSERVICE);

        RemoteRegistration registration = (RemoteRegistration) reg.lookup(RemoteRegistration.SERVICE_NAME);

        int x =registration.registra("Michele","Superman");

        if(x == -1)System.out.println("Utente Michele già registrato");

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

            boolean stop = false;
            int readyBytes;
            buffer = ByteBuffer.allocate(1024);
            while(!stop){
                buffer.clear();
                readyBytes = client.read(buffer);
                System.out.println("QUI");
                buffer.flip();
                response += new String(buffer.array());

                System.out.println(response);
                System.out.println(readyBytes);

                switch (response){
                    case "-2":
                        System.out.println("Utente non è registrato");
                        stop=true;
                        break;
                    case "-1":
                        System.out.println("Password errata");
                        stop=true;

                        break;
                    case "0":
                        System.out.println("Login già effettuato");
                        stop=true;

                        break;
                    case "1":
                        System.out.println("Login ok");
                        stop=true;

                        break;
                }
            }
      //  }


    }
}
