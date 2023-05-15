import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;

public class Server {
    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 3;

    private Selector selector;
    private List<Client> players;
    private ExecutorService executor;

    public Server() {
        players = new ArrayList<>();
        executor = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    public void start() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server is listening on port " + PORT);

            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Server exception: " + ex.getMessage());
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);

        Client newPlayer = new Client(clientChannel);
        players.add(newPlayer);

        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
        newPlayer.sendMessage("Waiting for the game to begin...");
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            // Connection closed by client
            Client disconnectedPlayer = findPlayerByChannel(clientChannel);
            players.remove(disconnectedPlayer);
            clientChannel.close();
            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
            return;
        }

        String message = new String(buffer.array(), 0, bytesRead).trim();
        Client currentPlayer = findPlayerByChannel(clientChannel);

        if (currentPlayer != null) {
            System.out.println("Received message from client " + clientChannel.getRemoteAddress() + ": " + message);
            handleGameLogic(currentPlayer, message);
        }
    }

    private void handleGameLogic(Client currentPlayer, String message) {
        // Process game logic based on the received message
        // In this example, assume the message is the client's play
        int play = Integer.parseInt(message);

        // Perform game logic and update player's score, etc.

        executor.execute(() -> {
            try {
                // Send game result back to the player
                currentPlayer.sendMessage("Game result: ...");
            } catch (IOException e) {
                System.err.println("Error sending message to client: " + e.getMessage());
                // Handle the exception accordingly
            }
        });
    }

    private Client findPlayerByChannel(SocketChannel clientChannel) {
        for (Client player : players)
            if (player.getChannel() == clientChannel)
                return player;

        return null;
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}