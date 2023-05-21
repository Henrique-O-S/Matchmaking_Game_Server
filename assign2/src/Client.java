// ---------------------------------------------------------------------------------------------------

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
        this.user = new User(channel);
        this.channel = channel;
        this.buffer = ByteBuffer.allocate(1024);
        this.scanner = new Scanner(System.in);

        this.signal_handler = new SignalHandler() {
            public void handle(Signal signal) {
                    System.out.println("\nDISCONNECTING...");
                    try {
                        writeMessage("error");
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

            String message = this.readMessage();
            if (message.equals("[INFO] You are connected"))
                this.connect();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

// ---------------------------------------------------------------------------------------------------

    public SocketChannel getChannel() {
        return this.channel;
    }

    public User getUser() {
        return this.user;
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

    private void connect() throws IOException {
        while (true) {
            System.out.println("'R' to REGISTER, 'L' to LOGIN, 'Q' to QUIT or '^C' to QUIT at anytime");
            String input[] = this.readInput();
            String s = input[0].toLowerCase();
            String message = "";

            switch (s) {
                case "r":
                    this.writeMessage("[CONNECT] " + s);
                    message = this.readMessage();
                    if (message.equals("[INFO] Registering"))
                        this.register();
                    break;
                case "l":
                    this.writeMessage("[CONNECT] " + s);
                    message = this.readMessage();
                    if (message.equals("[INFO] Logging in"))
                        this.login();
                    break;
                case "q":
                    System.out.println("\nGOOD BYE\n");
                    return;
                default:
                    System.out.println("Invalid input");
                    break;
            }
        }
    }

    private void register() throws IOException {
        while (true) {
            System.out.println("Register in this format: username password");
            String input[] = this.readInput();

            if (input.length != 2){
                System.out.println("Please attend to the requested format");
                System.out.println("Neither the username nor the password should contain spaces");
                return;
            }

            this.writeMessage("[REGISTER] " + input[0] + " " + input[1]);

            String message = this.readMessage();
            if (message.equals("[INFO] Added to queue"))
                this.queue();
            else if (message.equals("[INFO] User already exists, try again!"))
                System.out.println(message);
        }
    }

    private void login() throws IOException {
        while (true) {
            System.out.println("Login in this format: username password");
            String input[] = this.readInput();

            if (input.length != 2) {
                System.out.println("Please attend to the requested format");
                System.out.println("Neither the username nor the password should contain spaces");
                return;
            }

            this.writeMessage("[LOGIN] " + input[0] + " " + input[1]);

            String message = this.readMessage();
            if (message.equals("[INFO] Added to queue"))
                this.queue();
            else if (message.equals("[INFO] Invalid credentials, try again!"))
                System.out.println(message);
        }
    }

    private void queue() throws IOException {
        this.writeMessage("[QUEUE] ");

        while(true) {
            System.out.println("You were added to the queue");
            this.playGame();
        }
    }

    private void playGame() throws IOException {
        while (true) {
            String message = this.readMessage();

            String[] split_message = message.split("]");
            String identifier = split_message[0];

            switch (identifier) {
                case "[INFO":
                    System.out.println(message);
                    this.writeMessage("[INFO] Message received");
                    break;
                case "[PLAY":
                    System.out.println(message);
                    this.play();
                    break;
                case "[EXIT":
                    System.out.println("[EXIT] Game ended");
                    this.writeMessage("[INFO] Message received");
                    int score = Integer.parseInt(split_message[1].split("&")[1]);
                    System.out.println("Your updated score is " + score);
                    //this.playAgain();
                    return;
                default:
                    System.out.println("Invalid message");
                    return;
            }
        }
    }

    private void playAgain() throws IOException {
        System.out.println("Want to play again?");
        System.out.println("Hit 'Y' to continue playing, 'N' or '^C' to QUIT");
        String input[] = this.readInput();
        String s = input[0].toLowerCase();

        switch (s) {
            case "y":
                break;
            case "n":
                System.out.println("\nGOOD BYE\n");
                System.exit(0);              
                break;
            default:
                System.out.println("Invalid input");
                break;
        }
    }

// ---------------------------------------------------------------------------------------------------

private void play() {
    Timer timer = new Timer();
    AtomicBoolean timeoutOccurred = new AtomicBoolean(false); // flag to track timeout

    TimerTask timeoutTask = new TimerTask() {
        @Override
        public void run() {
            System.out.println("\nTimeout reached. Playing automatically...");
            rollDice();
            timeoutOccurred.set(true); // set the flag to true
        }
    };

    timer.schedule(timeoutTask, TIMEOUT);
    this.scanner.nextLine(); // wait for user to press Enter key
    timer.cancel(); // cancel the timeout task

    if (timeoutOccurred.get()) {
        return; // return if timeout occurred
    }

    // continue with the rest of the program
    rollDice();
    return;
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
            System.out.println("You got " + Integer.toString(play));
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// ---------------------------------------------------------------------------------------------------