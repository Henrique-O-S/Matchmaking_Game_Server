// ---------------------------------------------------------------------------------------------------

import java.util.*;
import java.io.*;
import java.lang.reflect.Field;

// ---------------------------------------------------------------------------------------------------

class Database {
    private String filename;
    private List<User> users;

    // ---------------------------------------------------------------------------------------------------

    public Database(String filename) throws FileNotFoundException, IOException {
        this.filename = filename;
        this.loadUsers();
    }

    private void loadUsers() throws FileNotFoundException, IOException {
        this.users = new ArrayList<User>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(" ");
                users.add(new User(columns[0], columns[1], Integer.parseInt(columns[2])));
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------

    public List<User> getUsers() {
        return this.users;
    }

    // ---------------------------------------------------------------------------------------------------

    public boolean registerUser(String[] user_data) throws IOException {
        for (User user : users)
            if (user.getUsername().equals(user_data[0]))
                return false;

        User newUser = new User(user_data[0], user_data[1], 1000);
        users.add(newUser);
        appendToFile(newUser);
        return true;
    }

    public User loginUser(String[] user_data) throws IOException {
        for (User user : users)
            if (user.getUsername().equals(user_data[0]) && user.getPassword().equals(user_data[1]))
                return user;

        return null;
    }

    // ---------------------------------------------------------------------------------------------------

    public void appendToFile(User new_user) throws IOException {
        Writer output = new BufferedWriter(new FileWriter(filename, true));
        output.append(new_user.getUsername() + " " + new_user.getPassword() + " " + new_user.getGlobalScore()
                + System.lineSeparator());
        output.close();
    }

    public void updateData(List<User> userList) throws IOException {
        try {
            FileWriter fileWriter = new FileWriter(filename, false);
            User selectedUser;
            ArrayList<User> newUsers = new ArrayList<User>();
            for (User user : this.users) {
                int index = getUserFromList(userList, user);
                if(index != -1){
                    selectedUser = userList.get(index);
                }
                else{
                    selectedUser = user;
                }
                newUsers.add(selectedUser);
                fileWriter.append(selectedUser.getUsername() + " " + selectedUser.getPassword() + " " + selectedUser.getGlobalScore() + System.lineSeparator());
            }
            fileWriter.close();
            this.users = newUsers;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int getUserFromList(List<User> userList, User user) {
        int index = 0;
        for (User el : userList) {
            if (el.getUsername().equals(user.getUsername())) {
                return index;
            }
            index++;
        }
        return -1;
    }
}

// ---------------------------------------------------------------------------------------------------