/**
 * @author Nicolò Maio
 *
 * Interfaccia per gestire l'operazione di richiesta registrazione in RMI
 * */
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistration extends Remote {

    public String SERVICE_NAME = "Registration";
    // nome del servizio offerto.

    /**
     * @param username username dell'utente  che dovrà essere registrato.
     * @param password password dell'utente  che dovrà essere registrato.
     * @return 1 se operazione andata a buon fine; 0 altrimenti.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    public int registra(String username, String password) throws RemoteException;

}
