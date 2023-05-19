// ---------------------------------------------------------------------------------------------------

import java.util.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// ---------------------------------------------------------------------------------------------------

public class Server {
    private static final int PORT = 5000;
    private static final int QUEUE_LIMIT = 5;
    private static final int MAX_GAMES = 1;
    private static final int GAME_CLIENTS = 3;

    private Queue<User> queue;
    private Database database;
    private Selector selector;
    private ByteBuffer buffer;

// ---------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Server server = new Server();
        server.launch();
    }

    public Server() throws FileNotFoundException, IOException {
        this.queue = new LinkedList<User>();
        this.database = new Database("data/users.txt");
        this.buffer = ByteBuffer.allocate(1024);
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

            this.selector = Selector.open();
            server_channel.register(this.selector, SelectionKey.OP_ACCEPT);

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
                    List<User> users = new ArrayList<User>();
                    while (users.size() < GAME_CLIENTS)
                        users.add(this.queue.poll());

                    for (User user : users) {
                        SelectionKey key = user.getClientChannel().keyFor(selector);
                        if (key != null)
                            key.cancel();
                    }   

                    // start new game
                    Game game = new Game(users);
                    executor.submit(game);

                    // game over
                    if (game.over()) {
                        for (User user : users) {
                            this.queue.add(user);
                            SocketChannel client_channel = user.getClientChannel();
                            client_channel.configureBlocking(false);
                            client_channel.register(selector, SelectionKey.OP_READ, user);
                        }

                        users.clear();
                    }
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

        User user = new User();
        user.setClientChannel(client_channel);

        client_channel.configureBlocking(false);
        client_channel.register(this.selector, SelectionKey.OP_READ, user);

        System.out.println("New client channel: " + client_channel.getRemoteAddress());
    }

    private void keyRead(SelectionKey key) throws IOException {
        SocketChannel client_channel = (SocketChannel)key.channel();

        String message = this.readMessage(client_channel);

        if (message.equals("error")) {
            this.disconnectClient(client_channel);
        }
        else {
            User user = (User)key.attachment();

            String[] split_message = message.split("]");
            String identifier = split_message[0];
            String content = "";

            switch (identifier) {
                case "[CONNECT":
                    if(message.equals("r"))
                        user.updateFlag("REG");
                    else if(message.equals("l"))
                        user.updateFlag("LOG");
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[REGISTER":
                    content = split_message[1];
                    this.registerClient(content, user, client_channel);
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[REG-ERROR":
                    content = split_message[1];
                    this.registerClient(content, user, client_channel);
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[LOGIN":
                    content = split_message[1];
                    this.loginClient(content, user, client_channel);
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[LOG-ERROR":
                    content = split_message[1];
                    this.loginClient(content, user, client_channel);
                    key.interestOps(SelectionKey.OP_WRITE);
                    break;
                case "[QUEUE":
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
                this.writeMessage(client_channel, "Message received");
                key.interestOps(SelectionKey.OP_READ);
                break;
            case "REG":
                this.writeMessage(client_channel, "Message received");
                key.interestOps(SelectionKey.OP_READ);
                break;
            case "R-ERR":
                this.writeMessage(client_channel, "User already exists, try again!");
                key.interestOps(SelectionKey.OP_READ);
                break;
            case "LOG":
                this.writeMessage(client_channel, "Message received");
                key.interestOps(SelectionKey.OP_READ);
                break;
            case "L-ERR":
                this.writeMessage(client_channel, "Invalid credentials, try again!");
                key.interestOps(SelectionKey.OP_READ);
                break;
            case "WQ":
                break;
            default:
                key.interestOps(SelectionKey.OP_READ);
                break;
        }
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

    private void disconnectClient(SocketChannel channel) throws IOException {
        // to do
    }

    private void registerClient(String content, User user, SocketChannel client_channel) throws IOException {
        String data[] = content.trim().split("\\s+");

        if (this.database.registerUser(data)) {
            user.setUsername(data[0]);
            user.setPassword(data[1]);
            user.updateFlag("WQ");

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
            user.updateFlag("WQ");

            System.out.println("User [" + user.getUsername() + "] has connected from client " + client_channel.getRemoteAddress());
        }
        else
            user.updateFlag("L-ERR");
    }
}

// ---------------------------------------------------------------------------------------------------