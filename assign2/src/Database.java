import java.io.*;
import java.util.ArrayList;

class Database {

    private String filename;
    private ArrayList<User> users;
    public Database(String filename) throws FileNotFoundException, IOException {
        this.filename = filename;
        this.loadUsers();
    }

    private void loadUsers() throws FileNotFoundException, IOException {
        users = new ArrayList<User>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(" ");
                users.add(new User(columns[0], columns[1], Integer.parseInt(columns[2])));
            }
        }    
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public boolean registerUser(String[] userData) throws IOException{
        for (User user : users) {
            if(user.getUsername().equals(userData[0])){
                return false;
            }
        }
        User newUser = new User(userData[0], userData[1], 0);
        users.add(newUser);
        appendToFile(newUser);
        return true;
    }

    public User loginUser(String[] userData) throws IOException{
        for (User user : users) {
            if(user.getUsername().equals(userData[0]) && user.getPassword().equals(userData[1])){
                return user;
            }
        }
        return null;
    }

    public void appendToFile(User newUser) throws IOException{
        Writer output = new BufferedWriter(new FileWriter(filename, true));
        output.append(newUser.getUsername() + " " + newUser.getPassword() + " " + newUser.getScore() + System.lineSeparator());
        output.close();
    }
}
