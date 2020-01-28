import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ServerCallBImpl extends RemoteObject implements ServerInterface {

    private TreeMap<String,NotifyEventInterface> clients;

    public ServerCallBImpl() throws RemoteException {
        super();
        clients = new TreeMap<>();
    }

    public synchronized void registerForCallback(String username,NotifyEventInterface ClientInterface) throws RemoteException {
        if(!clients.containsKey(username)){
            clients.put(username,ClientInterface);
        }
    }

    public synchronized int unregisterForCallback(String username,NotifyEventInterface Client) throws RemoteException {
        if(clients.remove(username,Client) ) return 1;
        return 0;
    }

    public void update(int value,String username) throws RemoteException{

        doCallback(value,username);
    }

    private synchronized void doCallback(int value,String username) throws RemoteException {
        if(clients.containsKey(username)){
            clients.get(username).notifyChallenge(value);

        }
    }

}
