import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {

    public void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException;

    public int unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException;


    public String SERVICE_NAME = "CallBack_Challenge";
}
