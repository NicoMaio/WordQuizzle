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

    public ServerCallBImpl(){
        super();
        clients = new TreeMap<>();
    }

    /**
     * @param username username dell'utente che si vuole aggiungere a clients.
     * @param ClientInterface interfaccia della classe del cliente che si vuole eseguire in caso di invio notifica di sfida.
     */
    public synchronized void registerForCallback(String username,NotifyEventInterface ClientInterface){
        if(!clients.containsKey(username)){
            clients.put(username,ClientInterface);
        }
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
