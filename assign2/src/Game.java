import java.util.*;

class Game {
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

    public void play() {
        System.out.println("Starting game with " + this.numPlayers + " players");
        for (int i = 0; i < this.numPlayers; i++)
            this.players[i].sendMessage("Starting game with " + this.numPlayers + " players");

        for (int round = 1; round < ROUNDS; round++)
            playRound(round);

        int max = playerScores[0];
        int winner = 0;
        for (int player = 1; player < this.numPlayers; player++) {
            if (playerScores[player] > max) {
                max = this.playerScores[player];
                winner = player;
            }
        }

        System.out.println("Player " + winner + " won the game!");
    }

    private void playRound(int round) {
        System.out.println("Round " + round);
        for (int i = 0; i < this.numPlayers; i++)
            this.players[i].sendMessage("Round " + round);

        int[] roundScores = new int[this.numPlayers];
        for (int player = 0; player < this.numPlayers; player++) {
            this.players[player].sendMessage("It's your turn:");
            int play = 1; // get a play from player channel

            roundScores[player] = play;
            this.playerScores[player] += play;
            System.out.println("Player " + player + " got " + play);
        }

        int max = roundScores[0];
        int winner = 0;
        for (int player = 1; player < this.numPlayers; player++) {
            if (roundScores[player] > max) {
                max = roundScores[player];
                winner = player;
            }
        }

        System.out.println("Player " + winner + " won this round");
        for (int i = 0; i < this.numPlayers; i++)
            this.players[i].sendMessage("Player " + winner + " won this round");
    }
}