import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;

public class Game implements Runnable {
    private static final int ROUNDS = 5;

    private List<User> players;
    private int num_players;
    private int[] player_scores;
    private ByteBuffer buffer;

    public Game(List<User> players) {
        this.players = players;
        this.num_players = players.size();
        this.player_scores = new int[this.num_players];
        this.buffer = ByteBuffer.allocate(1024);

        // initialize scores
        for (int i = 0; i < this.num_players; i++)
            this.player_scores[i] = 0;
    }

    @Override
    public void run() {
        System.out.println("Game started");
        try {
            for (User player : this.players)
                this.writeMessage(player.getClientChannel(), "[INFO] Game started");
            this.messageDelivered();
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        for (int round = 1; round <= ROUNDS; round++)
            this.playRound(round);
            
        List<User> winners = this.getWinners();

        String s = "Player(s): ";
        for(User player : winners)
            s += player.getUsername() + " ";
        s += "won the game!\n";

        System.out.println(s);
        try {
            for (User player : this.players)
                this.writeMessage(player.getClientChannel(), "[INFO] " + s);
            this.messageDelivered();
        } 
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // update player ranks
        for(User player : players)
            player.defeat();

        for(User player : winners)
            player.victory();

        System.out.println("Game ended");
        try {
            for (User player : this.players)
                this.writeMessage(player.getClientChannel(), "[EXIT]");
            this.messageDelivered();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void playRound(int round) {
        System.out.println("Round " + round);
        try {
            for (User player : this.players)
                this.writeMessage(player.getClientChannel(), "[INFO] Round " + round);
            this.messageDelivered();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        int[] round_scores = new int[this.num_players];
        for (int player = 0; player < this.num_players; player++) {
            try {
                round_scores[player] = this.getPlay(player);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        int highscore = round_scores[0];
        List<User> winners = new ArrayList<>();

        for (int player = 1; player < this.num_players; player++)
            if (round_scores[player] > highscore)
                highscore = round_scores[player];

        for (int player = 1; player < this.num_players; player++)
            if (round_scores[player] == highscore)
                winners.add(players.get(player));

        String s = "Player(s): ";
        for(User player : winners)
            s += player.getUsername() + " ";
        s += "won this round!\n";

        System.out.println(s);
        try {
            for (User player : this.players)
                this.writeMessage(player.getClientChannel(), "[INFO] " + s);
            this.messageDelivered();
        } 
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    } 

    private int getPlay(int player) throws IOException {
        this.writeMessage(this.players.get(player).getClientChannel(), "[PLAY] Press ENTER to roll the dice");

        SocketChannel player_channel = this.players.get(player).getClientChannel();

        while (true) {
            this.buffer.clear();
            int bytes_read = player_channel.read(this.buffer);
            String message = new String(this.buffer.array(), 0, bytes_read).trim();
            this.buffer.flip();
            
            int play = Integer.parseInt(message);
            if (play >= 1 && play <= 12) {
                System.out.println("Player " + player + " got " + play);
                return play;
            }
        }
    }

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

    private void messageDelivered() throws IOException, InterruptedException {
        int ends = 0;
        while (ends < this.num_players) {
            for (User player : this.players) {
                while (true) {
                    if (this.readMessage(player.getClientChannel()).equals("OK")) {
                        ends++;
                        break;
                    }
                }
            }
        }

        Thread.sleep(1000);
    }

    private List<User> getWinners() {
        List<User> winners = new ArrayList<>();
        int highscore = this.player_scores[0];

        for (int player = 1; player < this.num_players; player++)
            if (player_scores[player] > highscore)
                highscore = player_scores[player];

        for (int player = 1; player < this.num_players; player++)
            if (player_scores[player] == highscore)
                winners.add(players.get(player));

        return winners;
    }
}