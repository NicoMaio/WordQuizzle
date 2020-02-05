/**
 * @author Nicolò Maio
 *
 * Classe usata per gestire servizio di Callback in caso di richiesta di sfida lato client.
 * */
import java.rmi.server.RemoteObject;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {

    public NotifyEventImpl(){
        super();
    }

    /**
     * @param port porta alla quale dovrà essere inviata la risposta alla richiesta di sfida.
     */
    public void notifyChallenge(int port){

        try {
            MainClassClient.runExec(port);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
