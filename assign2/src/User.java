// ---------------------------------------------------------------------------------------------------

import java.nio.channels.*;

// ---------------------------------------------------------------------------------------------------

public class User {
    private String username;
    private String password;
    private int global_score;
    private SocketChannel client_channel;
    private int curr_play;
    private String flag;

// ---------------------------------------------------------------------------------------------------

    public User(SocketChannel client_channel) {
        this.username = "";
        this.password = "";
        this.global_score = 0;
        this.client_channel = client_channel;
        this.curr_play = 0;
        this.flag = "CON";
    }

    public User(String username, String password, int global_score) {
        this.username = username;
        this.password = password;
        this.global_score = global_score;
        this.curr_play = 0;
        this.flag = "CON";
    }

// ---------------------------------------------------------------------------------------------------

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

    public int currentPlay() {
        return this.curr_play;
    }

    public String getFlag() {
        return this.flag;
    }

// ---------------------------------------------------------------------------------------------------

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setClientChannel(SocketChannel client_channel) {
        this.client_channel = client_channel;
    }

    public void setPlay(int value) {
        this.curr_play = value;
    }

    public void updateFlag(String flag) {
        this.flag = flag;
    }

// ---------------------------------------------------------------------------------------------------

    public void victory() {
        this.global_score += 120;
    }

    public void defeat() {
        if (this.global_score < 20)
            this.global_score = 0;
        else
            this.global_score -= 20;
    }
}

// ---------------------------------------------------------------------------------------------------