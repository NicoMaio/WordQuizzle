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
    void registerForCallback(String username,NotifyEventInterface ClientInterface) throws RemoteException;


    String SERVICE_NAME = "CallBack_Challenge";
    // nome del servizio di Callback.
}
