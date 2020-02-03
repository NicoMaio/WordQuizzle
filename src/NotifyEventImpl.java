/**
 * @author Nicolò Maio
 *
 * Classe usata per gestire servizio di Callback in caso di richiesta di sfida lato client.
 * */
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {

    public NotifyEventImpl() throws RemoteException{
        super();
    }

    /**
     * @param port porta alla quale dovrà essere inviata la risposta alla richiesta di sfida.
     * @throws RemoteException eccezione lanciata in caso di vari errori.
     */
    public void notifyChallenge(int port) throws RemoteException {

        try {
            MainClassClient.runExec(port);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
