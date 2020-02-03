/**
 * @author Nicolò Maio
 *
 * Classe per gestire le richieste di registrazione tramite RMI.
 * */

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.TreeMap;

public class ImplRemoteRegistration extends UnicastRemoteObject implements RemoteRegistration {

    final TreeMap<String, Utente> registeredList;
    // TreeMap degli utenti registrati

    private Counters counters;
    // Istanza di Counters dell'utente che si sta registrando

    public ImplRemoteRegistration(TreeMap<String, Utente> albero,Counters counters) throws RemoteException {

        registeredList = albero;

        this.counters = counters;
    }

    /**
     * @param username username dell'utente che si sta registrando.
     * @param password password dell'utente che si sta registrando.
     * @return resituisce 1 se operazione andata a buon fine altrimenti 0.
     * @throws RemoteException eccezione che potrebbe essere lanciata.
     */
    public int registra(String username, String password) throws RemoteException {
        if(password == null) return 0;

        synchronized (registeredList) {
            if (registeredList.containsKey(username)) return -1;

            registeredList.put(username, new Utente(username, password,0));
        }
        counters.addUser(username);

        ServerService.saveUsersStats(registeredList);

        return 1;
    }


}
