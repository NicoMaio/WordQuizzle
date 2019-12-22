import java.util.Vector;

public class Elemento {

    private String username;

    private String password;

    private Vector<String> friends;

    public Elemento(String username, String password) {

        this.username = username;
        this.password = password;
        friends = new Vector<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword () {
        return password;
    }
}
