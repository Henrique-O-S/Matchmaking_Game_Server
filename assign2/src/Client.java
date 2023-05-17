import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;

public class Client {
    private SocketChannel channel;

    private User user;

    private Scanner scanner;

    private static final int PORT = 5002;

    private static final int TIMEOUT = 5000;

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
        return this.channel;
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
                case PLAYING: {
                    while(true){
                        message = readMessage();
                        System.out.println("SERVER" + "-" + message);
                        String[] splitMessage = message.split("-");
                        String identifier = splitMessage[0];
                        if(identifier.equals("INFO")){
                            System.out.println(message);
                            buffer.clear();
                            buffer.put("OK".getBytes());
                            buffer.flip();
                            channel.write(buffer);
                        } else if(identifier.equals("PLAY")){
                            System.out.println(message);
                            
                            Timer timer = new Timer();
        
                            TimerTask timeoutTask = new TimerTask() {
                                @Override
                                public void run() {
                                    System.out.println("\nTimeout reached. Playing automatically...");
                                    Random random = new Random();
                                    int play = random.nextInt(12) + 1;
                                    buffer.clear();
                                    buffer.put(Integer.toString(play).getBytes());
                                    buffer.flip();
                                    try {
                                        channel.write(buffer);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            timer.schedule(timeoutTask, TIMEOUT);
                            
                            scanner.nextLine(); // Wait for user to press Enter
                            timer.cancel(); // Cancel the timeout task
                            
                            // Continue with the rest of the program
                            Random random = new Random();
                            int play = random.nextInt(12) + 1;
                            buffer.clear();
                            buffer.put(Integer.toString(play).getBytes());
                            buffer.flip();
                            channel.write(buffer);
                          
                        }else if(identifier.equals("EXIT")){
                            System.out.println("Exiting back to waiting queue");
                            buffer.clear();
                            buffer.put("OK".getBytes());
                            buffer.flip();
                            channel.write(buffer);
                            user.setState(User.State.WAITING_QUEUE);
                            break;
                        }

                        
                    }
                }

            }
        }

        System.out.println("GOOD BYE!");

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
