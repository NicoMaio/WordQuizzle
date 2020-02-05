/**
 * @author Nicolò Maio
 *
 * Interfaccia per il servizio di Callback usato per gestire le notifiche agli sfidati.
 * */

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {


    /**
     * @param port porta alla quale dovrà essere indirizzata la risposta del client.
     * @throws RemoteException eccezione che potrebbere essere sollevata.
     */
    void notifyChallenge(int port) throws RemoteException;
}
