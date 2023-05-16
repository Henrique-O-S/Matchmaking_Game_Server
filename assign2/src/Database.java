import java.io.*;
import java.util.ArrayList;

class Database {

    private String filename;
    private ArrayList<User> users;
    public Database(String filename) {
        this.filename = filename;
    }

    public ArrayList<User> loadUsers() throws FileNotFoundException, IOException {
        users = new ArrayList<User>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(" ");
                users.add(new User(columns[0], columns[1], Float.parseFloat(columns[2])));
            }
        }    
        return users;
    }
}
