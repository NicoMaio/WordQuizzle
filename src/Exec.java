import java.rmi.RemoteException;

public class Exec implements Runnable {

    int port;
    public Exec(int port){

        this.port = port;
    }

    public void run(){
        try {
            MainClassClient.listenUDPreq(port);
        } catch (Exception r){

        }
    }
}
