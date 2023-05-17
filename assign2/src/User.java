import java.nio.channels.SocketChannel;

public class User {

    private String username = "";
    private String password = "";
    private Integer globalScore;
    private SocketChannel channel;

    private State state;

    public enum State{
        CONNECTED,
        REGISTERING,
        REGISTERING_ERROR,
        LOGGING,
        LOGGING_ERROR,
        AUTHENTICATED,
        WAITING_QUEUE,
        DISCONNECTED
    }

    public User(String username) {
        this.username = username;
        this.state = State.CONNECTED;
    }
    public User(String username, String password, Integer score) {
        this.username = username;
        this.password = password;
        this.globalScore = score;
        this.state = State.CONNECTED;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void setScore(Integer score){
        this.globalScore = score;
    }

    public String getUsername(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public Integer getScore(){
        return this.globalScore;
    }

    public State getState(){
        return state;
    }

    public void setState(State state){
        this.state = state;
    }

    public void setSocketChannel(SocketChannel channel){
        this.channel = channel;
    }

}
