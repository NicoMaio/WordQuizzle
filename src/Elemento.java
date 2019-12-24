import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

public class Elemento {

    private String username;

    private int point;
    private String password;

    private Vector<String> friends;

    public Elemento(String username, String password,int point) {

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

    public int getPoint(){
        return point;
    }

    public Vector<String> getFriends(){
        return friends;
    }

}
