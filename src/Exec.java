/**
 * @author Nicolò Maio
 *
 * Classe Exec del task eseguito da un client in caso di ricezione richiesta di sfida
 * */
public class Exec implements Runnable {

    int port;
    public Exec(int port){

        this.port = port;
    }

    public void run(){
        try {
            MainClassClient.listenUDPreq(port);
        } catch (Exception r){

        }
    }
}
