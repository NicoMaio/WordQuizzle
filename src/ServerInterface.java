import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {

    public void registerForCallback(String username,NotifyEventInterface ClientInterface) throws RemoteException;

    public int unregisterForCallback(String username,NotifyEventInterface ClientInterface) throws RemoteException;


    public String SERVICE_NAME = "CallBack_Challenge";
}
