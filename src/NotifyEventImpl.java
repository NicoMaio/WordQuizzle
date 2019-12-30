import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {

    public NotifyEventImpl() throws RemoteException{
        super();
    }

    public void notifyChallenge(int port) throws RemoteException {

        try {
            MainClassClient.listenUDPreq(port);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
