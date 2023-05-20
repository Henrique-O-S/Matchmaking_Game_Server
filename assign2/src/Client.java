// ---------------------------------------------------------------------------------------------------

import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import sun.misc.Signal;
import sun.misc.SignalHandler;

// ---------------------------------------------------------------------------------------------------

public class Client {
    private static final int PORT = 5002;
    private static final int TIMEOUT = 10000;

    private User user;
    private SocketChannel channel;
    private ByteBuffer buffer;
    private Scanner scanner;
    private SignalHandler signal_handler;

// ---------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(PORT));

        new Client(channel).launch();
    }

    public Client(SocketChannel channel) {
        this.user = new User();
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(1024);
        this.scanner = new Scanner(System.in);

        this.signal_handler = new SignalHandler() {
            public void handle(Signal signal) {
                    System.out.println("\nDISCONNECTING...");
                    try {
                        writeMessage("q");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("\nGOOD BYE\n");
                        System.exit(0);
                    }
                }
            };
    }

// ---------------------------------------------------------------------------------------------------

    private void launch() {
        try {
            Signal.handle(new Signal("INT"), this.signal_handler);

            while (true)
                if (!this.connect())
                        break;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

// ---------------------------------------------------------------------------------------------------

    public SocketChannel getChannel() {
        return this.channel;
    }

// ---------------------------------------------------------------------------------------------------

    private String[] readInput() {
        String input = this.scanner.nextLine();
        return input.split("\\s+");
    }

    public String readMessage() throws IOException {
        this.buffer.clear();
        int bytes_read = this.channel.read(this.buffer);
        this.buffer.flip();

        if (bytes_read >= 0)
            return new String(this.buffer.array(), 0, bytes_read).trim();

        return "error";
    }

    public void writeMessage(String message) throws IOException {
        this.buffer.clear();
        this.buffer.put(message.getBytes());
        this.buffer.flip();

        this.channel.write(this.buffer);
    }

// ---------------------------------------------------------------------------------------------------

    private boolean connect() throws IOException {
        System.out.println("'R' to REGISTER, 'L' to LOGIN, or '^C' to QUIT (at anytime)");

        String message = "";
        String input[] = this.readInput();
        String s = input[0].toLowerCase();

        switch (s) {
            case "r":
                this.writeMessage("[CONNECT] " + s);
                message = this.readMessage();
                if (message.equals("[INFO] Message received"))
                    if (!this.register())
                        return false;
                break;
            case "l":
                this.writeMessage("[CONNECT] " + s);
                message = this.readMessage();
                if (message.equals("[INFO] Message received"))
                    if (!this.login())
                        return false;
                break;
            case "q":
                this.writeMessage("[CONNECT] " + s);
                message = this.readMessage();
                if (message.equals("[INFO] Message received")) {
                    System.out.println("Goodbye!");
                    return false;
                }
                break;
            default:
                System.out.println("Invalid input");
                return false;
        }

        return true;
    }

    private boolean register() throws IOException {
        System.out.println("Register in this format: username password");
        String input[] = this.readInput();

        if (input.length != 2){
            System.out.println("Please attend to the requested format");
            System.out.println("Neither the username nor the password should contain spaces");
            return false;
        }

        this.writeMessage("[REGISTER] " + input[0] + " " + input[1]);
        String message = this.readMessage();
        if (!message.equals("[INFO] Message received")) {
            System.out.println(message);
            return false;
        }

        System.out.println("Registration complete!");

        while (true) 
            if (!this.queue())
                break;
        
        return true;
    }

    private boolean login() throws IOException {
        System.out.println("Login in this format: username password");
        String input[] = this.readInput();

        if (input.length != 2) {
            System.out.println("Please attend to the requested format");
            System.out.println("Neither the username nor the password should contain spaces");
            return false;
        }

        this.writeMessage("[LOGIN] " + input[0] + " " + input[1]);
        String message = this.readMessage();
        if (!message.equals("[INFO] Message received")) {
            System.out.println(message);
            return false;
        }

        System.out.println("Authentication complete");

        while (true) 
            if (!this.queue())
                break;

        return true;
    }

    private boolean queue() throws IOException {
        this.writeMessage("[QUEUE] ");

        String message = this.readMessage();
        if (!message.equals("[INFO] Message received")) {
            System.out.println(message);
            return false;
        }

        System.out.println("You were added to the queue");
        return this.playGame();
    }

    private boolean playGame() throws IOException {
        while (true) {
            String message = this.readMessage();
            System.out.println(message);

            String[] split_message = message.split("]");
            String identifier = split_message[0];

            switch (identifier) {
                case "[INFO":
                    this.writeMessage("[INFO] Message received");
                    break;
                case "[PLAY":
                    this.play();
                    break;
                case "[EXIT":
                    System.out.println("Your updated score is " + this.user.getGlobalScore());
                    System.out.println("Returning to queue");
                    this.writeMessage("[INFO] Message received");
                    return true;
                default:
                    System.out.println("Invalid message");
                    break;
            }
        }
    }

// ---------------------------------------------------------------------------------------------------

    private void play() {
        Timer timer = new Timer();
        TimerTask timeoutTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("\nTimeout reached. Playing automatically...");
                rollDice();
            }
        };

        timer.schedule(timeoutTask, TIMEOUT);             
        this.scanner.nextLine(); // wait for user to press Enter key
        timer.cancel(); // cancel the timeout task    

        // continue with the rest of the program
        rollDice();
    }

    private void rollDice() {
        Random random = new Random();
        int play = random.nextInt(12) + 1;
        String message = "[PLAY] " + Integer.toString(play);

        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        try {
            channel.write(buffer);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// ---------------------------------------------------------------------------------------------------