package client_server;

import game.Game;
import java.io.*;
import java.net.*;
import java.util.*;

public class MultiClientServer {
 
    public static void main(String[] args) {
        if (args.length < 1) return;
        
        while(true){

            int port = Integer.parseInt(args[0]);
            int maxClients = 3; // Maximum number of clients to handle concurrently
            int rounds = 10; // Number of rounds to play
            int[] points = new int[maxClients]; // Points for each client
            int connectedClients = 0; // Number of connected clients
            List<Socket> userSockets = new ArrayList<Socket>();
    
            try (ServerSocket serverSocket = new ServerSocket(port)) {
    
                System.out.println("Server is listening on port " + port);
                
                while (connectedClients < maxClients) {
                    Socket socket = serverSocket.accept();
                    OutputStream output = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(output, true);
                    System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
                    userSockets.add(socket);
                    writer.println("Waiting for the game to begin...");
                    connectedClients++;
                }

                for(int i = 0; i < connectedClients; i++){
                    OutputStream output = userSockets.get(i).getOutputStream();
                    PrintWriter writer = new PrintWriter(output, true);
                    writer.println("Game is starting");
                }

                Game game = new Game(connectedClients, userSockets);
                game.start();

                
            } catch (IOException ex) {
                System.out.println("Server exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}
