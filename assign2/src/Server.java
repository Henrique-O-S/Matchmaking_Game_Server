import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 5000;
    private static final int QUEUE_LIMIT = 10;
    private static final int MAX_GAMES = 2;
    private static final int GAME_CLIENTS = 1;

    private Selector selector;
    private ByteBuffer buffer;
    private Queue<User> queue;

    public static void main(String[] args) {
        Server server = new Server();
        server.launch();
    }

    public Server() {
        this.buffer = ByteBuffer.allocate(1024);
        this.queue = new LinkedList<User>();
    }

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

    private void keyAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server_channel = (ServerSocketChannel)key.channel();
        SocketChannel client_channel = server_channel.accept();

        client_channel.configureBlocking(false);

        User user = new User();
        user.setClientChannel(client_channel);

        client_channel.register(this.selector, SelectionKey.OP_WRITE, user);
        System.out.println("New client connected from: " + client_channel.getRemoteAddress());

        if (this.queue.size() <= QUEUE_LIMIT)
            this.queue.add(user);
    }

    private void keyRead(SelectionKey key) throws IOException {
        SocketChannel client_channel = (SocketChannel)key.channel();

        this.buffer.clear();
        int bytes_read = client_channel.read(this.buffer);
        this.buffer.flip();

        if (bytes_read == -1) {
            // disconnect client
        }
        else {
            User user = (User)key.attachment();
            String message = new String(this.buffer.array(), 0, bytes_read).trim();

            // to do
        }
    }

    private void keyWrite(SelectionKey key) throws IOException {
        SocketChannel client_channel = (SocketChannel)key.channel();
        User user = (User)key.attachment();

        // to do
    }

    private void writeMessage(SocketChannel channel, String message) throws IOException {
        this.buffer.clear();
        this.buffer.put(message.getBytes());
        this.buffer.flip();
        
        channel.write(this.buffer);
    }
}
