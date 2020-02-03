/**
 * @author Nicolò Maio
 *
 * Classe usata per gestire servizio di Callback lato server.
 * */
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.TreeMap;

public class ServerCallBImpl extends RemoteObject implements ServerInterface {

    private TreeMap<String,NotifyEventInterface> clients;
    // TreeMap contenente i riferimenti dei clienti per notificare le richieste di sfida.

    public ServerCallBImpl() throws RemoteException {
        super();
        clients = new TreeMap<>();
    }

    /**
     * @param username username dell'utente che si vuole aggiungere a clients.
     * @param ClientInterface interfaccia della classe del cliente che si vuole eseguire in caso di invio notifica di sfida.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    public synchronized void registerForCallback(String username,NotifyEventInterface ClientInterface) throws RemoteException {
        if(!clients.containsKey(username)){
            clients.put(username,ClientInterface);
        }
    }

    /**
     * @param username username dell'utente che si vuole deregistrare dal servizio di notifiche di sfida.
     * @param Client interfaccia della classe del cliente che si vuole eliminare dal servizio di notifiche di sfida.
     * @return 1 se operazione andata a buon fine; 0 altrimenti.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    public synchronized int unregisterForCallback(String username,NotifyEventInterface Client) throws RemoteException {
        if(clients.remove(username,Client) ) return 1;
        return 0;
    }

    /**
     * @param value conterrà la porta alla quale inviare la risposta di richiesta di sfida.
     * @param username username dell'utene al quale si invierà la richiesta di sfida.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    public void update(int value,String username) throws RemoteException{

        doCallback(value,username);
    }

    /**
     * @param value conterrà la porta alla quale inviare la risposta di richiesta di sfida.
     * @param username username dell'utene al quale si invierà la richiesta di sfida.
     * @throws RemoteException eccezione che potrebbe essere sollevata.
     */
    private synchronized void doCallback(int value,String username) throws RemoteException {
        if(clients.containsKey(username)){
            clients.get(username).notifyChallenge(value);

        }
    }

}
