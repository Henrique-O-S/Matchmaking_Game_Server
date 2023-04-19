import java.net.*;
import java.io.*;
import java.util.Random;
 
/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class TestClient {
 
    public static void main(String[] args) {
        if (args.length < 2) return;
 
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
 
        try (Socket socket = new Socket(hostname, port)) {
            
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            int points = 0;
            boolean running = true;
            while(running){

                String continue_connection = reader.readLine();

                if ("stop".equals(continue_connection)){
                    running = false;
                }
                else{
                    //writer.println("new Date()?".toString());

                    // Instance of random class
                    Random rand = new Random(); 
                    // Setting the upper bound to generate the
                    // random numbers in specific range
                    int upperbound = 11;
                    // Generating random values from 0 - 11
                    // using nextInt()
                    int int_random = rand.nextInt(upperbound); 

                    System.out.println(int_random);

                    writer.println(int_random);
        
                    String result = reader.readLine();
        
                    System.out.println(result);

                    if("You won".equals(result)){
                        points++;
                    }
                }
            }
            writer.println(points);
            System.out.print("You got a total of ");
            System.out.print(points);
            System.out.println(" points.");
 
        } catch (UnknownHostException ex) {
 
            System.out.println("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}