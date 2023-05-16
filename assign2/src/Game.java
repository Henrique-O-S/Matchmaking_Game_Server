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
        //STARTING GAME
        System.out.println("\nStarting game with " + this.numPlayers + " players\n");
        try {
            sendToAllClients(players, "INFO" + " " + "Starting game with " + this.numPlayers + " players\n");

            readConfirmationFromClients(players);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //PLAYING ROUNDS
        for (int round = 1; round < ROUNDS; round++)
            playRound(round);

        //SENDING GAME RESULTS
        List<Client> winners = getWinners();
        System.out.println("Player(s): ");

        for(Client player : winners){
            System.out.println(player);
        }

        System.out.println("Won the game\n");
        try {
            this.sendToAllClients(players, "INFO" + " " + "Player(s): ");

            readConfirmationFromClients(players);

            for(Client player : winners){
                this.sendToAllClients(players, "INFO" + " " + player.toString());

                readConfirmationFromClients(players);
            }

            this.sendToAllClients(players, "INFO" + " " + "Won the game\n");

            readConfirmationFromClients(players);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playRound(int round) {
        //SIGNALING ROUND
        System.out.println("[Round " + round + "]");
        try {
            sendToAllClients(players, "INFO" + " " + "[Round " + round + "]\n");

            readConfirmationFromClients(players);
        } catch (IOException e) {
            e.printStackTrace(); 
        }

        //GETTING USERS PLAYS
        int[] roundScores = new int[this.numPlayers];
        for (int player = 0; player < this.numPlayers; player++) {
            try {
                roundScores[player] = getPlay(player);
                //readConfirmationFromClients(players);
            } catch (IOException e) {
                e.printStackTrace(); 
            }
        }
            
        int highscore = roundScores[0];
        List<Client> winners = new ArrayList<>();
        for (int player = 1; player < this.numPlayers; player++) {
            if (roundScores[player] > highscore) {
                highscore = roundScores[player];
            }
        }

        for (int player = 1; player < this.numPlayers; player++) {
            if (roundScores[player] == highscore) {
                winners.add(players.get(player));
            }
        }

        //SENDING ROUND RESULTS
        System.out.println("Player(s): ");

        for(Client player : winners){
            System.out.println(player);
        }

        System.out.println("Won this round\n");

        try {
            this.sendToAllClients(players, "INFO" + " " + "Player(s): ");

            readConfirmationFromClients(players);

            for(Client player : winners){
                this.sendToAllClients(players, "INFO" + " " + player.toString());

                readConfirmationFromClients(players);
            }

            this.sendToAllClients(players, "INFO" + " " + "Won this round\n");

            readConfirmationFromClients(players);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Client> getWinners() {
        int highscore = this.playerScores[0];
        List<Client> winners = new ArrayList<>();
        for (int player = 1; player < this.numPlayers; player++) {
            if (playerScores[player] > highscore) {
                highscore = playerScores[player];
            }
        }

        for (int player = 1; player < this.numPlayers; player++) {
            if (playerScores[player] == highscore) {
                winners.add(players.get(player));
            }
        }

        return winners;
    }

    private int getPlay(int player) throws IOException {
        this.players.get(player).sendMessage("PLAY" + " " + "[Type a number] > ");

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

    private void sendToAllClients(List<Client> players,String message)throws IOException{

        System.out.println("Send to all clients: " + message);
        
        for(Client player : players){
           player.sendMessage(message);
        }
    }

    private void readConfirmationFromClients(List<Client> players)throws IOException{

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int players_ready = 0;

        while (true) {
            
            for(Client player : players){

                while(true){
                    SocketChannel channel = player.getSocketChannel();
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    String response = new String(buffer.array(), 0, buffer.limit()).trim();


                    if(response.equals("OK")){
                        players_ready++;
                        System.out.println(response);
                        System.out.println("Players Ready: " + players_ready);
                        System.out.println("User: "+player.toString()+" is ready");
                        player.sendMessage("OK");
                        break;
                    }
                }
                

            }
            if(players_ready == players.size()){
                System.out.println("Proceding");
                return;
            }
        }
    }
}