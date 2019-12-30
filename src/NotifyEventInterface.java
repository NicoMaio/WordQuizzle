import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {

    public void notifyChallenge(int port) throws RemoteException;
}
