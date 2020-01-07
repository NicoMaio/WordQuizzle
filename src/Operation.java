import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Arrays;

public class Operation {

    RemoteRegistration registration;
    ServerInterface server;
    SocketChannel client;

    public Operation(RemoteRegistration registration, ServerInterface server, SocketChannel client){
        this.registration = registration;
        this.server = server;
        this.client = client;
    }

    public int registra(String username,char[] password) throws RemoteException {
       String passw = Arrays.toString(password);
       return registration.registra(username,passw);
    }

    public String login(String username,char[] password) throws IOException {
        String passw = Arrays.toString(password);

        String input = "login/"+username+"/"+passw;

        ByteBuffer buffer = ByteBuffer.wrap(input.getBytes());

        client.write(buffer);
        buffer.clear();
        buffer.flip();

        String response="";


        ByteBuffer fer = ByteBuffer.allocate(1024);

        client.read(fer);
        fer.flip();

        response += StandardCharsets.UTF_8.decode(fer).toString();


        return response;




    }
}
