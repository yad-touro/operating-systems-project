

/**
 * Clients are going to connect directly to the master and submit jobs of either type (A or B). The client’s submission
 * should include the type, and an ID number that will be used to identify the job throughout the system.
 */

import java.net.*;
import java.io.*;


public class Client {

    public static int clientNum = 0;

    public static void main(String[] args) {

//        String clientName = "Client " + clientNum;
//        clientNum++;

        // Hardcode in IP and Port here if required
        args = new String[] {"127.0.0.1", "30121"};

        if (args.length != 2) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        boolean masterHasCompletedJobsForClient = false;
        String hostName = args[0];
        int outgoingPortNumber = Integer.parseInt(args[1]);

        try (
                Socket outgoingClientSocket = new Socket(hostName, outgoingPortNumber);
                //Socket incomingClientSocket = new Socket(hostName, incomingPortNumber);

                PrintWriter outgoingWriteToServer = // stream to write text requests to server
                        new PrintWriter(outgoingClientSocket.getOutputStream(), true);
                BufferedReader outgoingReadFromServer = // stream to read text response from server
                        new BufferedReader(
                                new InputStreamReader(outgoingClientSocket.getInputStream()));
                BufferedReader userInput = // standard input stream to get user's requests
                        new BufferedReader(
                                new InputStreamReader(System.in));

        ) {

            System.out.println("Connecting to Master");
            System.out.println("Master Responded: " + outgoingReadFromServer.readLine());  //  This line is the initial "welcome" message from Master.

            while (outgoingClientSocket != null) {  // This is where we'll ask the Client for a Job ID, and a Job type


                //
                System.out.println("Master Asks: " + outgoingReadFromServer.readLine());
                outgoingWriteToServer.println(userInput.readLine());

                System.out.println("Master Asks: " + outgoingReadFromServer.readLine());
                outgoingWriteToServer.println(userInput.readLine());

                System.out.println("Master Responded " + outgoingReadFromServer.readLine());
                System.out.println("Master Responded " + outgoingReadFromServer.readLine());

                System.out.println("Asking Master if any of my Jobs have completed");
                masterHasCompletedJobsForClient = Boolean.parseBoolean(outgoingReadFromServer.readLine());
                while (masterHasCompletedJobsForClient) {
                    System.out.println("Master responds: Job " + outgoingReadFromServer.readLine());
                    masterHasCompletedJobsForClient = Boolean.parseBoolean(outgoingReadFromServer.readLine());
                }
                System.out.println("Master responds: No more completed Jobs for you");


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