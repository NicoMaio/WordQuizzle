/**
 * @author Nicolò Maio
 *
 * Interfaccia per gestire notifiche in Callback per richieste di sfida lato server.
 * */
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {

    /**
     * @param username username dell'utente che si vuole aggiungere a clients.
     * @param ClientInterface interfaccia della classe del cliente che si vuole eseguire in caso di invio notifica di sfida.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    public void registerForCallback(String username,NotifyEventInterface ClientInterface) throws RemoteException;

    /**
     * @param username username dell'utente che si vuole deregistrare dal servizio di notifiche di sfida.
     * @param ClientInterface interfaccia della classe del cliente che si vuole eliminare dal servizio di notifiche di sfida.
     * @return 1 se operazione andata a buon fine; 0 altrimenti.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    public int unregisterForCallback(String username,NotifyEventInterface ClientInterface) throws RemoteException;


    public String SERVICE_NAME = "CallBack_Challenge";
    // nome del servizio di Callback.
}
