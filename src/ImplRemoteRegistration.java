import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.TreeMap;


public class ImplRemoteRegistration extends UnicastRemoteObject implements RemoteRegistration {

    final TreeMap<String, Utente> registeredList;

    public ImplRemoteRegistration(TreeMap<String, Utente> albero) throws RemoteException {

        registeredList = albero;

    }

    public int registra(String username, String password) throws RemoteException {
        if(password == null) return 0;

        synchronized (registeredList) {
            if (registeredList.containsKey(username)) return -1;

            registeredList.put(username, new Utente(username, password,0));
        }
        return 1;
    }


}
