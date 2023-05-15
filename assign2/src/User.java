public class User {

    private String username = "";
    private String password = "";
    private Float globalScore;
    public User() {}
    public User(String username, String password, Float score) {
        this.username = username;
        this.password = password;
        this.globalScore = score;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public void setScore(Float score){
        this.globalScore = score;
    }

    public String getUsername(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public Float getScore(){
        return this.globalScore;
    }

}
