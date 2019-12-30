import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ServerCallBImpl extends RemoteObject implements ServerInterface {

    private List<NotifyEventInterface> clients;

    public ServerCallBImpl() throws RemoteException {
        super();
        clients = new ArrayList<NotifyEventInterface>();
    }

    public synchronized void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
        if(!clients.contains(ClientInterface)){
            clients.add(ClientInterface);
        }
    }

    public synchronized int unregisterForCallback(NotifyEventInterface Client) throws RemoteException {
        if(clients.remove(Client) ) return 1;
        return 0;
    }

    public void update(int value, NotifyEventInterface Client) throws RemoteException{
        doCallback(value,Client);
    }

    private synchronized void doCallback(int value, NotifyEventInterface Client) throws RemoteException {
        if(clients.contains(Client)){
            Client.notifyChallenge(value);
        }
    }

}
