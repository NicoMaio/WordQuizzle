import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ShutDownerThread extends Thread {

    private String username;
    private SocketChannel client;
    public ShutDownerThread(String username, SocketChannel client){
        this.username = username;
        this.client = client;
    }
    public void run(){
        String toServer = "logout/" + username;

        ByteBuffer buffer = ByteBuffer.wrap(toServer.getBytes());
        try {
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
        }catch (IOException e){
            e.printStackTrace();
        }


    }
}

