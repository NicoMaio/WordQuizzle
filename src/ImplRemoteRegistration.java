import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.TreeMap;


public class ImplRemoteRegistration extends UnicastRemoteObject implements RemoteRegistration {

    final TreeMap<String, Elemento> registeredList;

    final TreeMap<String, Integer> pointsClassification;
    public ImplRemoteRegistration() throws RemoteException {

        registeredList = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareTo(s2);
            }
        });

        pointsClassification = new TreeMap<>();
    }

    public int registra(String username, String password) throws RemoteException {
        if(password == null) return 0;

        synchronized (registeredList) {
            if (registeredList.containsKey(username)) return -1;

            registeredList.put(username, new Elemento(username, password));
        }

        synchronized (pointsClassification) {
            pointsClassification.put(username,0);

        }

        return 1;
    }


}
