import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.TreeMap;


public class ImplRemoteRegistration extends UnicastRemoteObject implements RemoteRegistration {

    final TreeMap<String, Elemento> registeredList;

    public ImplRemoteRegistration(TreeMap<String,Elemento> albero) throws RemoteException {

        registeredList = albero;

    }

    public int registra(String username, String password) throws RemoteException {
        if(password == null) return 0;

        synchronized (registeredList) {
            if (registeredList.containsKey(username)) return -1;

            registeredList.put(username, new Elemento(username, password,0));
        }
        return 1;
    }


}
