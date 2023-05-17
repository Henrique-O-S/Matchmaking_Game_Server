import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

class Client {
    private SocketChannel channel;

    private User user;

    private Scanner scanner;

    private static final int PORT = 5002;

    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    public Client(SocketChannel channel) {
        this.channel = channel;
        this.scanner = new Scanner(System.in);
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public String sendMessageReceiveResponse(String message) throws IOException {
        buffer.clear();
        buffer.put(message.getBytes());

        buffer.flip();
        channel.write(buffer);

        buffer.clear();
        
        ByteBuffer response = ByteBuffer.allocate(1024);

        int bytesRead = channel.read(response);
        if (bytesRead >= 1) {
            return new String(response.array(), 0, response.limit()).trim();
        }
        return "error";
    }

    public String readMessage() throws IOException {
        buffer.clear();
        int bytesRead = channel.read(buffer);
        buffer.flip();
        if (bytesRead >= 0) {
            return new String(buffer.array(), 0, buffer.limit()).trim();
        }
        return "error";
    }

    public void start() throws IOException {
        this.user = new User("Default");
        String message;
        String data[];
        while (this.user.getState() != User.State.DISCONNECTED) {
            switch (this.user.getState()) {
                case CONNECTED: {
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    String response = new String(buffer.array(), 0, buffer.limit()).trim();
                    if (response.equals("Connection Established")) {
                        System.out.println("'R' to REGISTER, 'L' to LOGIN, or 'Q' to QUIT");
                        data = readInput();
                        if (data[0].toLowerCase().equals("r")) {
                            message = sendMessageReceiveResponse(data[0]);
                            if (message.equals("OK")) {
                                this.user.setState(User.State.REGISTERING);
                            }
                        }

                        else if (data[0].toLowerCase().equals("l")) {
                            message = sendMessageReceiveResponse(data[0]);
                            System.out.println(message);
                            if (message.equals("OK")) {
                                this.user.setState(User.State.LOGGING);
                            }
                        }

                        else if (data[0].toLowerCase().equals("q")) {
                            message = sendMessageReceiveResponse(data[0]);
                            this.user.setState(User.State.DISCONNECTED);

                        }
                    }
                    else{
                        System.out.println("error, disconnecting");
                        this.user.setState(User.State.DISCONNECTED);
                    }

                    break;
                }
                case REGISTERING: {
                    System.out.println("Enter the desired data in this format: username password");
                    data = readInput();
                    if(data.length != 2){
                        System.out.println("Please attend to the requested format");
                        System.out.println("Both username and password cant have spaces");
                        break;
                    }
                    message = sendMessageReceiveResponse(data[0] + " " + data[1]);
                    if(!message.equals("OK")){
                        System.out.println(message);
                        break;
                    }
                    System.out.println("Registration Complete!");
                    this.user.setState(User.State.AUTHENTICATED);
                    break;
                }
                case LOGGING: {
                    System.out.println("username password?");
                    data = readInput();
                    if(data.length != 2){
                        System.out.println("Please attend to the requested format");
                        System.out.println("Both username and password cant have spaces");
                        break;
                    }
                    message = sendMessageReceiveResponse(data[0] + " " + data[1]);
                    if(!message.equals("OK")){
                        System.out.println(message);
                        break;
                    }
                    System.out.println("Authentication Complete!");
                    this.user.setState(User.State.AUTHENTICATED);
                    break;
                }
                case AUTHENTICATED: {
                }

            }
        }

        System.out.println("GODD BYE!");

        channel.close();
    }

    public static void main(String[] args) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(PORT));
        new Client(channel).start();
    }

    private String[] readInput(){
        String input = scanner.nextLine();
        return input.split("\\s+");
    }

}
