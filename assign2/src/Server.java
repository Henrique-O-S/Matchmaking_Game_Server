// ---------------------------------------------------------------------------------------------------

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import sun.misc.Signal;
import sun.misc.SignalHandler;
import java.security.SecureRandom;

// ---------------------------------------------------------------------------------------------------

public class Server {
    private static final int PORT = 5002;
    private static final int QUEUE_LIMIT = 5;
    private static final int MAX_GAMES = 1;
    private static final int GAME_CLIENTS = 2;
    private static final int TOKEN_BYTE_NUMBER = 10;
    private static final int SEARCH_REFRESH_RATE = 60000;

    private List<Client> clients;
    private Queue<User> queue;

    private List<User> activeUsers;

    private Database database;
    private Selector selector;
    private ByteBuffer buffer;
    private SignalHandler signal_handler;

// ---------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Server server = new Server();
        server.launch();
    }

    public Server() throws FileNotFoundException, IOException {
        this.clients = new ArrayList<Client>();
        
        this.queue = new LinkedList<User>();
        this.activeUsers = new ArrayList<User>();

        this.database = new Database("data/users.txt");
        this.buffer = ByteBuffer.allocate(1024);

        this.signal_handler = new SignalHandler() {
                public void handle(Signal signal) {
                    System.out.println("\nCLOSING...");
                    try {
                        for (Client client : clients)
                            client.getChannel().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("\nSERVER CLOSED\n");
                        System.exit(0);
                    }
                }
            };
    }

// ---------------------------------------------------------------------------------------------------

    private void launch() {
        try {
            ServerSocketChannel server_channel = ServerSocketChannel.open();
            server_channel.bind(new InetSocketAddress(PORT));
            System.out.println("Server is listening on port " + PORT);
            server_channel.configureBlocking(false);

            // thread pool
            ExecutorService executor = Executors.newFixedThreadPool(MAX_GAMES);

            // signal handler
            Signal.handle(new Signal("INT"), this.signal_handler);

            this.selector = Selector.open();
            server_channel.register(this.selector, SelectionKey.OP_ACCEPT);

            long start_time = System.currentTimeMillis();

            int search_limits = 200;

            while (true) {
                this.selector.select(1000);
                
                Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid())
                        key.cancel();
                    else if (key.isAcceptable())
                        this.keyAccept(key);
                    else if (key.isReadable())
                        this.keyRead(key);
                    else if (key.isWritable())
                        this.keyWrite(key);
                }

                if (this.queue.size() >= GAME_CLIENTS) {
                    int base_score = -1;
                    int max_score = -1;
                    int min_score = -1;
                    int players_in_limits = 0;

                    outerSearch: for(User user : this.queue){
                        if (this.activeUsers.contains(user)){
                            // set limits based on current client
                            base_score = user.getGlobalScore();
                            max_score = base_score + search_limits;
                            if(base_score - search_limits > 0)
                                min_score = base_score - search_limits;
                            else
                                min_score = 0;

                            players_in_limits = 0;
                            for(User other_user : this.queue){
                                if(other_user.getGlobalScore() >= min_score && other_user.getGlobalScore() <= max_score && this.activeUsers.contains(other_user)){
                                    players_in_limits++;
                                }

                                if(players_in_limits >= GAME_CLIENTS){
                                    break outerSearch;
                                }
                            }
                        }
                    }

                    if(players_in_limits >= GAME_CLIENTS){
                        List<User> users = new ArrayList<User>();
                        while (users.size() < GAME_CLIENTS) {
                            for(User user : this.queue){
                                if(user.getGlobalScore() >= min_score && user.getGlobalScore() <= max_score && this.activeUsers.contains(user)){
                                    users.add(this.queue.poll());
                                }
                            }
                        }

                        for (User user : users) {
                            SelectionKey key = user.getClientChannel().keyFor(this.selector);
                            if (key != null) {
                                key.cancel();
                                key.attach(null);
                            }
                        }   

                        // start new game
                        Game game = new Game(users);
                        Future<?> game_future = executor.submit(game);

                        // reset search timer
                        start_time = System.currentTimeMillis();

                        // reset search limits
                        search_limits = 200;

                        try {
                            game_future.get(); // wait for game to finish
                        }
                        catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }

                        if (game.over()) {
                            ArrayList<User> updated = new ArrayList<>();

                            for (User user : users) {
                                SocketChannel client_channel = user.getClientChannel();
                                client_channel.configureBlocking(false);

                                SelectionKey key = client_channel.keyFor(this.selector);
                                if (key != null) {
                                    try {
                                        int interestOps = key.interestOps();
                                        key.interestOps(interestOps | SelectionKey.OP_READ);
                                        key.attach(user);
                                    } catch (CancelledKeyException e) {
                                        // something
                                    }
                                }

                                updated.add(user);
                            }

                            database.updateData(updated);
                        }
                    }
                }

                // update search limits
                if(System.currentTimeMillis() - start_time > SEARCH_REFRESH_RATE){
                    start_time = System.currentTimeMillis(); // reset search timer
                    search_limits += 200;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

// ---------------------------------------------------------------------------------------------------

    private void keyAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server_channel = (ServerSocketChannel)key.channel();
        SocketChannel client_channel = server_channel.accept();
        client_channel.configureBlocking(false);

        User user = new User(client_channel);
        Client client = new Client(client_channel);
        clients.add(client);

        client_channel.register(this.selector, SelectionKey.OP_WRITE, user);

        System.out.println("New client channel: " + client_channel.getRemoteAddress());
    }

    private void keyRead(SelectionKey key) throws IOException {
        SocketChannel client_channel = (SocketChannel)key.channel();

        String message = this.readMessage(client_channel);

        if (message.equals("error")) {
            this.disconnectClient(key);
        }
        else {
            User user = (User)key.attachment();

            String[] split_message = message.split("]");
            String identifier = split_message[0];
            String content = "";

            switch (identifier) {
                case "[CONNECT":
                    //System.out.println("CONNECT READ");
                    content = split_message[1].trim();
                    if(content.equals("r"))
                        user.updateFlag("REG");
                    else if(content.equals("l"))
                        user.updateFlag("LOG");
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[REGISTER":
                    //System.out.println("REGISTER READ");
                    content = split_message[1];
                    this.registerClient(content, user, client_channel);
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[LOGIN":
                    //System.out.println("LOGIN READ");
                    content = split_message[1];
                    this.loginClient(content, user, client_channel);
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[QUEUE":
                    System.out.println("QUEUE READ");
                    this.queue.add(user);
                    break;
                default:
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
            }
        }
    }

    private void keyWrite(SelectionKey key) throws IOException {
        SocketChannel client_channel = (SocketChannel)key.channel();
        User user = (User)key.attachment();

        switch (user.getFlag()) {
            case "CON":
                //System.out.println("CON WRITE");
                this.writeMessage(client_channel, "[INFO] You are connected");
                break;
            case "REG":
                //System.out.println("REG WRITE");
                this.writeMessage(client_channel, "[INFO] Registering");
                break;
            case "R-ERR":
                //System.out.println("R-ERR WRITE");
                this.writeMessage(client_channel, "[INFO] User already exists, try again!");
                break;
            case "LOG":
                //System.out.println("LOG WRITE");
                this.writeMessage(client_channel, "[INFO] Logging in");
                break;
            case "L-ERR":
                //System.out.println("L-ERR WRITE");
                this.writeMessage(client_channel, "[INFO] Invalid credentials, try again!");
                break;
            case "WQ":
                //System.out.println("WQ WRITE");
                this.writeMessage(client_channel, "[INFO] Added to queue");
                break;
            default:
                break;
        }

        key.interestOps(SelectionKey.OP_READ);
    }

// ---------------------------------------------------------------------------------------------------

    private String readMessage(SocketChannel channel) throws IOException {
        this.buffer.clear();
        int bytes_read = channel.read(this.buffer);
        this.buffer.flip();

        if (bytes_read >= 0)
            return new String(this.buffer.array(), 0, bytes_read).trim();

        return "error";
    }

    private void writeMessage(SocketChannel channel, String message) throws IOException {
        this.buffer.clear();
        this.buffer.put(message.getBytes());
        this.buffer.flip();
        
        channel.write(this.buffer);
    }

// ---------------------------------------------------------------------------------------------------

    private void disconnectClient(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel)key.channel();
        User user = (User)key.attachment();
        Client client = this.getClient(channel);
        System.out.println("Client disconnected: " + channel.getRemoteAddress());
        this.clients.remove(client);
        this.activeUsers.remove(user);
        channel.close();
    }

    private void registerClient(String content, User user, SocketChannel client_channel) throws IOException {
        String data[] = content.trim().split("\\s+");

        if (this.database.registerUser(data)) {
            user.setUsername(data[0]);
            user.setPassword(data[1]);
            user.updateFlag("WQ");
            activeUsers.add(user);
            System.out.println("User [" + user.getUsername() + "] has connected from client " + client_channel.getRemoteAddress());
        }
        else
            user.updateFlag("R-ERR");
    }

    private void loginClient(String content, User user, SocketChannel client_channel) throws IOException {
        String data[] = content.trim().split("\\s+");

        User u = this.database.loginUser(data);
        if (u != null) {
            user.setUsername(u.getUsername());
            user.setPassword(u.getPassword());
            user.setGlobalScore(u.getGlobalScore());
            user.updateFlag("WQ");
            activeUsers.add(user);
            System.out.println("User [" + user.getUsername() + "] has connected from client " + client_channel.getRemoteAddress());

        }
        else
            user.updateFlag("L-ERR");
    }

// ---------------------------------------------------------------------------------------------------

    private Client getClient(SocketChannel client_channel) {
        for (Client client : this.clients)
            if (client.getChannel() == client_channel)
                return client;

        return null;
    }

    private byte[] getRandomBytes() {
        byte[] bytes = new byte[TOKEN_BYTE_NUMBER];

        SecureRandom secure_random = new SecureRandom();
        secure_random.nextBytes(bytes);

        return bytes;
    }

}

// ---------------------------------------------------------------------------------------------------