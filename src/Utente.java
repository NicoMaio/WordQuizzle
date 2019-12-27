import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

public class Utente {

    private String username;

    private long point;
    private String password;

    private Vector<String> friends;

    public Utente(String username, String password, long point) {

        this.username = username;
        this.password = password;
        this.point = point;
        friends = new Vector<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword () {
        return password;
    }

    public void setFriend(String username){
        friends.add(username);
    }

    public void setPoint(int point){
        this.point = this.point+point;
    }

    public long getPoint(){
        return point;
    }

    public Vector<String> getFriends(){
        return friends;
    }

}
