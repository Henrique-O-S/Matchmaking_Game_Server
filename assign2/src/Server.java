import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import java.net.InetSocketAddress;

public class Server {
    private static final int PORT = 5002;
    private static final int MAX_CLIENTS = 3;

    private Selector selector;
    private List<Client> clients;
    private ExecutorService executor;
    private Database database;
    private List<User> users;

    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    public Server() throws FileNotFoundException, IOException {
        clients = new ArrayList<>();
        executor = Executors.newFixedThreadPool(MAX_CLIENTS);
        database = new Database("data/users.txt");
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Server server = new Server();
        server.start();
    }

    public void start() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            users = database.getUsers();
            
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                selector.select(1000);

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                }

                int users_ready = 0;
                boolean new_game = false;
                for (User user : users) {
                    if (user.getState() == User.State.QUEUE) {
                        users_ready++;
                        if (users_ready == MAX_CLIENTS) {
                            new_game = true;
                            break;
                        }
                    }
                }

                if (new_game) {
                    List<User> game_users = new ArrayList<>();
                    for (User user : users) {
                        if (users_ready == 0)
                            break;
                        else if (user.getState() == User.State.QUEUE) {
                            user.setState(User.State.PLAYING);
                            game_users.add(user);
                            users_ready--;
                            SelectionKey key = user.getSocketChannel().keyFor(selector);
                            if (key != null)
                                key.cancel();
                        }
                    }

                    Game game = new Game(game_users);
                    executor.submit(game);
                }
            }
        } catch (IOException ex) {
            System.err.println("Server exception: " + ex.getMessage());
        }
    }

    private void disconnectClient(SocketChannel channel) throws IOException{
        Client client = findClientByChannel(channel);
        clients.remove(client);
        System.out.println("Client disconnected: " + channel.getRemoteAddress());
        channel.close();
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        
        User user = new User("default");
        user.setState(User.State.CONNECTED);
        user.setSocketChannel(channel);

        Client newClient = new Client(channel);
        clients.add(newClient);

        
        channel.register(selector, SelectionKey.OP_WRITE, user);
        System.out.println("Client connected: " + channel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // Connection closed by client
            disconnectClient(clientChannel);
            return;
        }

        String message = new String(buffer.array(), 0, bytesRead).trim();
        Client currentClient = findClientByChannel(clientChannel);

        if (currentClient != null) {
            System.out.println("Received message from client " + clientChannel.getRemoteAddress() + ": " + message);
            //handleGameLogic(currentClient, message);
        }

        if(message.toLowerCase().equals("q")){
            disconnectClient(clientChannel);
            return;
        }

        User user = (User) key.attachment();

        switch(user.getState()){
            case CONNECTED:{
                if(message.toLowerCase().equals("r")){
                    user.setState(User.State.REGISTERING);
                }
                else if(message.toLowerCase().equals("l")){
                    user.setState(User.State.LOGGING);
                }
                break;
            }
            case REGISTERING:{
                String data[] = message.split("\\s+");
                if(database.registerUser(data)){
                    users = database.getUsers();
                    user.setUsername(data[0]);
                    user.setPassword(data[1]);
                    user.setScore(0);
                    user.setState(User.State.AUTHENTICATED);

                    System.out.println("User " + user.getUsername() + " has connected from client " + clientChannel.getRemoteAddress());
                }
                else{
                    user.setState(User.State.REGISTERING_ERROR);
                }
                break;
            }
            case REGISTERING_ERROR:{
                String data[] = message.split("\\s+");
                if(database.registerUser(data)){
                    users = database.getUsers();
                    user.setUsername(data[0]);
                    user.setPassword(data[1]);
                    user.setScore(0);
                    user.setState(User.State.AUTHENTICATED);
                }
                else{
                    user.setState(User.State.REGISTERING_ERROR);
                }
                break;
            }
            case LOGGING:{
                String data[] = message.split("\\s+");
                User user2 = database.loginUser(data);
                if(user2 != null){
                    user.setUsername(user2.getUsername());
                    user.setPassword(user2.getPassword());
                    user.setScore(user2.getScore());
                    user.setState(User.State.AUTHENTICATED);
                    System.out.println("User " + user.getUsername() + " has connected from client " + clientChannel.getRemoteAddress());
                }
                else{
                    user.setState(User.State.LOGGING_ERROR);
                }
                break;
            }
            case LOGGING_ERROR:{
                String data[] = message.split("\\s+");
                User user2 = database.loginUser(data);
                if(user2 != null){
                    user.setUsername(user2.getUsername());
                    user.setPassword(user2.getPassword());
                    user.setScore(user2.getScore());
                    user.setState(User.State.AUTHENTICATED);
                }
                break;
            }
            case AUTHENTICATED:{
                user.setState(User.State.QUEUE);
                break;
            }

        }
        if(!user.getState().equals(User.State.QUEUE))
        {
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();

        User user = (User) key.attachment();

        switch(user.getState()){
            case CONNECTED:{
                sendMessage(clientChannel, "Connection Established");
                break;
            }
            case REGISTERING:{
                sendMessage(clientChannel, "OK");
                break;
            }
            case REGISTERING_ERROR:{
                sendMessage(clientChannel, "User already exists, try again!");
                break;
            }
            case LOGGING_ERROR:{
                sendMessage(clientChannel, "Invalid credentials, try again!");
                break;
            }
            case LOGGING:{
                sendMessage(clientChannel, "OK");
                break;
            }
            case AUTHENTICATED:{
                sendMessage(clientChannel, "OK");
                break;
            }
            case QUEUE: {
                sendMessage(clientChannel, "OK");
                break;
            }
        }
        if(!user.getState().equals(User.State.QUEUE))
        {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private Client findClientByChannel(SocketChannel clientChannel) {
        for (Client client : clients)
            if (client.getChannel() == clientChannel)
                return client;

        return null;
    }

    private void sendMessage(SocketChannel channel, String message) throws IOException{
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        channel.write(buffer);
    }
}
