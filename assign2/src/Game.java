import java.util.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Game implements Runnable {
    private static final int ROUNDS = 5;

    private List<User> players;
    private int numPlayers;
    private int[] playerScores;

    public Game(List<User> players) {
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
            sendToAllUsers(players, "INFO" + "-" + "Starting game with " + this.numPlayers + " players\n");

            readConfirmationFromUsers(players);

            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        //PLAYING ROUNDS
        for (int round = 1; round < ROUNDS; round++)
            playRound(round);

        //SENDING GAME RESULTS
        List<User> winners = getWinners();
        System.out.println("Player(s): ");

        for(User player : winners){
            System.out.println(player);
        }

        System.out.println("Won the game\n");
        try {
            this.sendToAllUsers(players, "INFO" + "-" + "Player(s): ");

            readConfirmationFromUsers(players);

            for(User player : winners){
                this.sendToAllUsers(players, "INFO" + "-" + player.toString());

                readConfirmationFromUsers(players);
            }

            this.sendToAllUsers(players, "INFO" + "-" + "Won the game\n");

            readConfirmationFromUsers(players);

            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        try {
            this.sendToAllUsers(players, "EXIT");

            readConfirmationFromUsers(players);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playRound(int round) {
        //SIGNALING ROUND
        System.out.println("[Round " + round + "]");
        try {
            sendToAllUsers(players, "INFO" + "-" + "[Round " + round + "]\n");

            readConfirmationFromUsers(players);

            Thread.sleep(1000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(); 
        }

        //GETTING USERS PLAYS
        int[] roundScores = new int[this.numPlayers];
        for (int player = 0; player < this.numPlayers; player++) {
            try {
                roundScores[player] = getPlay(player);
                //readConfirmationFromUsers(players);
            } catch (IOException e) {
                e.printStackTrace(); 
            }
        }
            
        int highscore = roundScores[0];
        List<User> winners = new ArrayList<>();
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

        for(User player : winners){
            System.out.println(player);
        }

        System.out.println("Won this round\n");

        try {
            this.sendToAllUsers(players, "INFO" + "-" + "Player(s): ");

            readConfirmationFromUsers(players);

            for(User player : winners){
                this.sendToAllUsers(players, "INFO" + "-" + player.toString());

                readConfirmationFromUsers(players);
            }

            this.sendToAllUsers(players, "INFO" + "-" + "Won this round\n");

            readConfirmationFromUsers(players);

            Thread.sleep(1000);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<User> getWinners() {
        int highscore = this.playerScores[0];
        List<User> winners = new ArrayList<>();
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
        sendToUser(this.players.get(player), "PLAY" + "-" + "Press ENTER to roll the dice");

        SocketChannel channel = this.players.get(player).getSocketChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (true) {
            buffer.clear();
            int bytesRead = channel.read(buffer);
            String message = new String(buffer.array(), 0, bytesRead).trim();
            buffer.flip();
            
            int play = Integer.parseInt(message);
            if (play >= 1 && play <= 12) {
                System.out.println("Player " + player + " got " + play);
                return play;
            }
        }
    }

    private void sendToUser(User player,String message)throws IOException{
        
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
    
        SocketChannel channel = player.getSocketChannel();
        channel.write(buffer);
    }

    private void sendToAllUsers(List<User> players,String message)throws IOException{
        
        for(User player : players){
           sendToUser(player, message);
        }
    }

    private void readConfirmationFromUsers(List<User> players)throws IOException{

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int players_ready = 0;

        while (true) {
            
            for(User player : players){

                while(true){
                    SocketChannel channel = player.getSocketChannel();
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                    String response = new String(buffer.array(), 0, buffer.limit()).trim();


                    if(response.equals("OK")){
                        players_ready++;
                        System.out.println("User: " + player.getUsername() + " is ready");
                        System.out.println("Players Ready: " + players_ready);
                        break;
                    }
                }
                

            }
            if(players_ready == players.size()){
                System.out.println("Proceeding");
                return;
            }
        }
    }
}