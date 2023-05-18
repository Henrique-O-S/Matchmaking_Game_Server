import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.net.InetSocketAddress;

public class Client {
    private static final int PORT = 5000;
    private static final int TIMEOUT = 5000;

    private SocketChannel channel;
    private User user;
    private Scanner scanner;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.launch();
    }

    public Client() {
        this.scanner = new Scanner(System.in);
    }

    private void launch() {
        try {
            this.channel = SocketChannel.open();
            this.channel.connect(new InetSocketAddress(PORT));

            this.user = new User("default");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    private String[] readInput(){
        String input = this.scanner.nextLine();
        return input.split("\\s+");
    }

}
