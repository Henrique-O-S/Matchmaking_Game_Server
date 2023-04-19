import java.io.*;
import java.net.*;
import java.util.Date;
 
/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */
public class TestServer {
 
    public static void main(String[] args) {
        if (args.length < 1) return;
 
        int port = Integer.parseInt(args[0]);
 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
 
            System.out.println("Server is listening on port " + port);
            
            int rounds = 0;
            Socket socket = serverSocket.accept();
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            while (rounds < 10) {

                writer.println("continue");
 
                String number = reader.readLine();
                int int_number = Integer.parseInt(number);

                System.out.print("Client Result: ");
                System.out.println(number);
 
                if(int_number == 10)
                    writer.println("You won");
                else
                writer.println("You lost");

                rounds++;
            }
            writer.println("stop");

            String points = reader.readLine();
            int int_points = Integer.parseInt(points);

            System.out.print("The Client got a total of: ");
            System.out.print(points);
            System.out.println(" points.");
 
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}