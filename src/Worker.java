import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public class Worker implements Runnable {

    private final TreeMap<String, Utente> registeredList;
    private TreeMap<String, SocketAddress> usersList;
    private ServerService.Con con;
    private SelectionKey key;

    public Worker(ServerService.Con con,TreeMap<String, Utente> registeredList,
                  TreeMap<String, SocketAddress> usersList,SelectionKey key) {
        this.con  = con;
        this.registeredList = registeredList;
        this.usersList = usersList;
        this.key = key;
    }

    @Override
    public void run() {

        con.req.flip();
        String receive = StandardCharsets.UTF_8.decode(con.req).toString();
        System.out.println("ecco stringa ricevuta: "+receive);
        String[] elenco = receive.split("/");

        if(elenco.length >0){
            switch (elenco[0]){
                case "login" : // typeOp 1
                    String username = elenco[1];
                    String password = elenco[2];

                    Utente utente= null;
                    synchronized (registeredList) {
                        if (registeredList.containsKey(username)) {
                            utente = registeredList.get(username);
                        }
                    }
                    String response="";
                    /*
                    *   response = 0 : login già effettuato;
                    *   response = 1 : login ok;
                    *   response = -1 : password sbagliata;
                    *   responde = -2 : utente non registrato;
                    * */
                    if(utente!=null){
                        if(password.equals(utente.getPassword())){
                            boolean ok=false;
                            synchronized (usersList){
                                if(usersList.containsKey(utente.getUsername())){
                                    ok = true;
                                } else {
                                    usersList.put(username,con.sa);
                                }
                            }

                            if(ok){
                                // login già effettuato

                                response += "0";

                            } else {
                                // login ok
                                response+= "1";
                            }
                        } else {
                            // password sbagliata
                            response+= "-1";
                        }
                    } else {
                        // utente non è registrato
                        response+= "-2";
                    }

                    // salvo risposta per client in con.resp
                    con.typeOp = 1;
                    con.resp = ByteBuffer.wrap(response.getBytes());



                    System.out.println(new String(con.resp.array()));
                    key.attach(con);
                    key.interestOps(SelectionKey.OP_WRITE);
                break;
                case "logout": // typeOp 2
                    break;
                case "lista_amici":
                    break;
                case "aggiungi_amico":
                    break;
                case "mostra_punteggio":
                    break;
                case "mostra_classifica":
                    break;
                case "sfida":
                    break;
                default:
                    break;
            }
        }
        return;
    }

}
