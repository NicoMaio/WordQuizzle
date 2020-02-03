/**
 * @author Nicolò Maio
 *
 * Classe che conterrà info di un utente registrato.
 * */
import java.util.Vector;

public class Utente {

    private String username;
    // username dell'utente.

    private long point;
    // punteggio dell'utente.

    private String password;
    // password dell'utente.

    private Vector<String> friends;
    // Vector che conterrà la lista di amici dell'utente.

    public Utente(String username, String password, long point) {

        this.username = username;
        this.password = password;
        this.point = point;
        friends = new Vector<>();
    }

    /**
     * @return username dell'utente.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return password dell'utente.
     */
    public String getPassword () {
        return password;
    }

    /**
     * @param username username dell'utente da aggiungere a friends.
     */
    public void setFriend(String username){
        friends.add(username);
    }

    /**
     * @param point punteggio da aggiungere a point.
     */
    public void setPoint(int point){
        this.point = this.point+point;
    }

    /**
     * @return punteggio dell'utente
     */
    public long getPoint(){
        return point;
    }

    /**
     * @return friends contenente i nomi degli amici dell'utente.
     */
    public Vector<String> getFriends(){
        return friends;
    }

}
