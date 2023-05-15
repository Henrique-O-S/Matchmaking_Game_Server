import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {
    private static final int PORT = 5000;
    private SocketChannel channel;

    public static void main(String[] args) {
        client.start();
    }

    public Client(SocketChannel channel) {
        this.channel = channel;
    }

    public void start() {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(PORT));

        /*
        while (true) {
            // read message from server
            // send message to server
        }
        */

        channel.close();
    }

    public SocketChannel getChannel() {
        return this.channel;
    }

    public void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        this.channel.write(buffer);
    }
}
