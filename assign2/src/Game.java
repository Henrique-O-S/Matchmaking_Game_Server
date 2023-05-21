// ---------------------------------------------------------------------------------------------------

import java.util.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;

// ---------------------------------------------------------------------------------------------------

public class Game implements Runnable {
    private static final int ROUNDS = 3;

    private int id;
    private List<User> players;
    private int num_players;
    private ByteBuffer buffer;
    private boolean game_over;

// ---------------------------------------------------------------------------------------------------

    public Game(List<User> players) {
        this.players = players;
        this.num_players = players.size();
        this.buffer = ByteBuffer.allocate(1024);
        this.game_over = false;
    }

// ---------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        this.game_over = false;
        System.out.println("Game started");

        try {
            for (User player : this.players) {
                player.resetRoundsWon();
                this.writeMessage(player.getClientChannel(), "[INFO] Game started");
            }
            this.getFeedback(false);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        for (int round = 1; round <= ROUNDS; round++)
            this.playRound(round);
            
        List<User> winners = this.getWinners();

        String s = "Player(s) ";
        for (User player : winners) {
            s += player.getUsername() + " ";
        }
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
                this.writeMessage(player.getClientChannel(), "[EXIT] Game ended&" + player.getGlobalScore());
            this.getFeedback(false);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        this.game_over = true;
    }

// ---------------------------------------------------------------------------------------------------

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

        int highscore = -1;
        List<User> winners = new ArrayList<>();

        for (int player = 0; player < this.num_players; player++)
            if (round_scores[player] > highscore)
                highscore = round_scores[player];

        for (int player = 0; player < this.num_players; player++)
            if (round_scores[player] == highscore)
                winners.add(players.get(player));

        String s = "Player(s) ";
        for (User player : winners){
            player.roundVictory();
            s += player.getUsername() + " ";
        }
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

// ---------------------------------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------------------------------

    private void getFeedback(boolean playing) throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(this.num_players);
        for (User player : this.players) {
            CompletableFuture.runAsync(() -> {
                try {
                    while (true) {
                        if (!playing) {
                            if (readMessage(player.getClientChannel()).equals("[INFO] Message received"))
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
    }

// ---------------------------------------------------------------------------------------------------

    private List<User> getWinners() {
        List<User> winners = new ArrayList<>();
        int highest_rounds = -1;

        for (int player = 0; player < this.num_players; player++)
            if (this.players.get(player).roundsWon() > highest_rounds)
                highest_rounds = this.players.get(player).roundsWon();

        for (int player = 0; player < this.num_players; player++)
            if (this.players.get(player).roundsWon() == highest_rounds)
                winners.add(this.players.get(player));

        return winners;
    }

// ---------------------------------------------------------------------------------------------------

    public boolean over() {
        return this.game_over;
    }
}

// ---------------------------------------------------------------------------------------------------