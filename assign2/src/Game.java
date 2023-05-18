import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;

public class Game implements Runnable {
    private static final int ROUNDS = 2;

    private List<User> players;
    private int num_players;
    private int[] player_scores;
    private ByteBuffer buffer;
    private boolean game_over;

    public Game(List<User> players) {
        this.players = players;
        this.num_players = players.size();
        this.player_scores = new int[this.num_players];
        this.buffer = ByteBuffer.allocate(1024);
        this.game_over = false;

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
            this.getFeedback(false);
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
            this.getFeedback(false);
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
                this.writeMessage(player.getClientChannel(), "[EXIT] Game ended");
            this.getFeedback(false);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        this.game_over = true;
    }

    private void playRound(int round) {
        System.out.println("Round " + round);
        try {
            for (User player : this.players)
                this.writeMessage(player.getClientChannel(), "[INFO] Round " + round);
            this.getFeedback(false);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        try {
            for (User player : this.players)
                this.writeMessage(player.getClientChannel(), "[PLAY] Press ENTER to roll the dice");
            this.getFeedback(true);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        int[] round_scores = new int[this.num_players];
        for (int player = 0; player < this.num_players; player++)
            round_scores[player] = this.players.get(player).currentPlay();

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
            this.getFeedback(false);
        } 
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
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

    private void getFeedback(boolean playing) throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(this.num_players);
        for (User player : this.players) {
            CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        if (!playing) {
                            if (readMessage(player.getClientChannel()).equals("received"))
                                break;
                        }
                        else {
                            String message = this.readMessage(player.getClientChannel());
                            String[] split_message = message.split("]");
                            String identifier = split_message[0];

                            if (identifier.equals("[PLAY")) {
                                int play = Integer.parseInt(split_message[1].trim());
                                player.setPlay(play);
                                System.out.println("Player " + player.getUsername() + " got " + play);
                                break;
                            }
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // wait until the latch count reaches zero
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

    public boolean over() {
        return this.game_over;
    }
}