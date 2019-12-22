import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistration extends Remote {

    public String SERVICE_NAME = "Registration";

    public int registra(String username, String password) throws RemoteException;

}
