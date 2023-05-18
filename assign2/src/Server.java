import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 3;

    public static void main(String[] args) {
        Server server = new Server();
        server.launch();
    }

    public Server() {
        // to do
    }

    private void launch() {
        try {
            ServerSocketChannel server_channel = ServerSocketChannel.open();
            server_channel.bind(new InetSocketAddress(PORT));
            server_channel.configureBlocking(false);

            Selector selector = Selector.open();
            server_channel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server is listening on port " + PORT);

            while (true) {
                selector.select(1000);

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        // accept key
                    } else if (key.isReadable()) {
                        // read key
                    } else if (key.isWritable()) {
                        // write key
                    }

                    // start game
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
