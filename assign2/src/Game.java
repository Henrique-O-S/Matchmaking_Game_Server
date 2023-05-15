import java.util.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Game implements Runnable {
    private static final int ROUNDS = 5;

    private List<Client> players;
    private int numPlayers;
    private int[] playerScores;

    public Game(List<Client> players) {
        this.players = players;
        this.numPlayers = players.size();
        this.playerScores = new int[this.numPlayers];
        for (int i = 0; i < this.numPlayers; i++)
            this.playerScores[i] = 0;
    }

    @Override
    public void run() {
        System.out.println("\nStarting game with " + this.numPlayers + " players\n");

        for (int round = 1; round < ROUNDS; round++)
            playRound(round);

        int winner = getWinner();
        System.out.println("Player " + winner + " won the game!\n");
        for (int i = 0; i < this.numPlayers; i++) {
            try {
                this.players.get(i).sendMessage("Player " + winner + " won the game!\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playRound(int round) {
        System.out.println("[Round " + round + "]");
        for (int i = 0; i < this.numPlayers; i++) {
            try {
                this.players.get(i).sendMessage("[Round " + round + "]\n");
            } catch (IOException e) {
                e.printStackTrace(); 
            }
        }

        int[] roundScores = new int[this.numPlayers];
        for (int player = 0; player < this.numPlayers; player++) {
            try {
                roundScores[player] = getPlay(player);
            } catch (IOException e) {
                e.printStackTrace(); 
            }
        }
            
        int max = roundScores[0];
        int winner = 0;
        for (int player = 1; player < this.numPlayers; player++) {
            if (roundScores[player] > max) {
                max = roundScores[player];
                winner = player;
            }
        }

        System.out.println("Player " + winner + " won this round\n");
        for (int i = 0; i < this.numPlayers; i++) {
            try {
                this.players.get(i).sendMessage("Player " + winner + " won this round\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getWinner() {
        int max = this.playerScores[0];
        int winner = 0;
        for (int player = 1; player < this.numPlayers; player++) {
            if (this.playerScores[player] > max) {
                max = this.playerScores[player];
                winner = player;
            }
        }

        return winner;
    }

    private int getPlay(int player) throws IOException {
        this.players.get(player).sendMessage("[Type a number] > ");

        SocketChannel channel = this.players.get(player).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (true) {
            buffer.clear();
            int bytesRead = channel.read(buffer);
            String message = new String(buffer.array(), 0, bytesRead).trim();
            buffer.flip();
            
            int play = Integer.parseInt(message);
            if (play >= 1 && play <= 6) {
                System.out.println("Player " + player + " got " + play);
                return play;
            }
        }
    }
}