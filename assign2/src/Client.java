import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class Client {
    private SocketChannel channel;

    public Client(SocketChannel channel) {
        this.channel = channel;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        channel.write(buffer);
    }
}
