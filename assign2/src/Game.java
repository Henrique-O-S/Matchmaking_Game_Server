import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;

public class Game implements Runnable {
    private static final int ROUNDS = 5;

    private List<User> players;
    private int num_players;
    private int[] player_scores;
    ByteBuffer buffer;

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
        System.out.println("[Game started]");

        for (int round = 1; round <= ROUNDS; round++)
            this.playRound(round);
            
        List<User> winners = this.getWinners();
        String s = "";
        for(User player : winners)
            s += player.getUsername() + " ";
        System.out.println("Player(s): " + s + "won the game!\n");
    }

    private void playRound(int round) {
        System.out.println("[Round " + round + "]");

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

        String s = "";
        for(User player : winners)
            s += player.getUsername() + " ";
        System.out.println("Player(s): " + "won this round!\n");
    } 

    private int getPlay(int player) throws IOException {
        // send user instruction to play

        SocketChannel player_channel = this.players.get(player).getClientChannel();

        while (true) {
            buffer.clear();
            int bytesRead = player_channel.read(buffer);
            String message = new String(buffer.array(), 0, bytesRead).trim();
            buffer.flip();
            
            int play = Integer.parseInt(message);
            if (play >= 1 && play <= 12) {
                System.out.println("Player " + player + " got " + play);
                return play;
            }
        }
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