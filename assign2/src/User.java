import java.nio.channels.*;

public class User {
    private String username = "";
    private String password = "";
    private int global_score;
    private SocketChannel client_channel;

    public User(String username) {
        this.username = username;
    }

    public User(String username, String password, int global_score, SocketChannel client_channel) {
        this.username = username;
        this.password = password;
        this.global_score = global_score;
        this.client_channel = client_channel;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public int getGlobalScore() {
        return this.global_score;
    }

    public SocketChannel getClientChannel() {
        return this.client_channel;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password= password;
    }

    public void setGlobalScore(int global_score) {
        this.global_score = global_score;
    }

    public void setClientChannel(SocketChannel client_channel) {
        this.client_channel = client_channel;
    }
}