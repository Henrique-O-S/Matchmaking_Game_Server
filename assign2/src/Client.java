import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class Client {
    private SocketChannel channel;

    private User user;

    private ClientState state;

    public enum ClientState{
        CONNECTED,
        AUTHENTICATED
    }

    public Client(SocketChannel channel) {
        this.channel = channel;
        this.user = new User();
    }

    public void setUser(User user){
        this.user = user;
    }

    public ClientState getState(){
        return state;
    }

    public void setState(ClientState state){
        this.state = state;
    }

    public boolean isAuthenticated(){
        return this.user.getUsername() != "" && this.user.getPassword() != "";
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        channel.write(buffer);
    }
}
