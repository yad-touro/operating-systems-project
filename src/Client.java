/**
 * Clients are going to connect directly to the master and submit jobs of either type (A or B). The client’s submission
 * should include the type, and an ID number that will be used to identify the job throughout the system.
 */

import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        
        // Hardcode in IP and Port here if required
        args = new String[] {"127.0.0.1", "30121"};

        if (args.length != 2) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
                Socket clientSocket = new Socket(hostName, portNumber);
                PrintWriter writeToServer = // stream to write text requests to server
                        new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader readFromServer = // stream to read text response from server
                        new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));
                BufferedReader userInput = // standard input stream to get user's requests
                        new BufferedReader(
                                new InputStreamReader(System.in))
        ) {
            String userInputSendToServer;

            while (!(userInputSendToServer = userInput.readLine()).equals("exit") ) {  // This is where we'll ask the Client for a Job ID, and a Job type

                writeToServer.println(userInputSendToServer);                 // of type A or (exclusive or) B.
                System.out.println("Master Responded: " + readFromServer.readLine());  //  This line is the initial "welcome" message from Master.
                //
                System.out.println("Master Asks: " + readFromServer.readLine());
                writeToServer.println(userInput.readLine());

                System.out.println("Master Asks: " + readFromServer.readLine());
                writeToServer.println(userInput.readLine());

                System.out.println("Master Responded " + readFromServer.readLine());

            }

            if (userInputSendToServer.equals("exit")) {
                writeToServer.println(userInputSendToServer);
            }

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostName);
            System.exit(1);
        }  // end catch

    }  // end of main

}  //  end of class