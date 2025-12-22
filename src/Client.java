/**
 * Clients are going to connect directly to the master and submit jobs of either type (A or B). The client’s submission
 * should include the type, and an ID number that will be used to identify the job throughout the system.
 */

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

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
            System.out.println("Client: Connected to Master");
            
            // Message queue to safely pass messages between threads
            BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
            
            // Create a separate thread to continuously read from server
            Thread messageReader = new Thread(() -> {
                try {
                    String message;
                    while ((message = readFromServer.readLine()) != null) {
                        if (message.startsWith("JOB_COMPLETE:")) {
                            // Handle completion notification immediately
                            String[] parts = message.split(":");
                            if (parts.length == 3) {
                                String jobType = parts[1];
                                String jobId = parts[2];
                                System.out.println("Client: Master notified that job " + jobType + ":" + jobId + " is complete");
                                System.out.println("Client: Job " + jobType + ":" + jobId + " completed successfully");
                            }
                        } else {
                            // Put regular messages in queue for main thread
                            messageQueue.put(message);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    // Connection closed or error
                }
            });
            messageReader.setDaemon(true);
            messageReader.start();

            while (true) {
                try {
                    // Read welcome message from Master (from queue)
                    String message = messageQueue.take();
                    System.out.println("Master: " + message);
                    
                    // Read Job ID prompt (from queue)
                    message = messageQueue.take();
                    System.out.println("Master: " + message);
                    String jobId = userInput.readLine();
                    if (jobId != null && jobId.equals("exit")) {
                        writeToServer.println(jobId);
                        break;
                    }
                    System.out.println("Client: Sending Job ID: " + jobId + " to Master");
                    writeToServer.println(jobId);

                    // Read Job Type prompt (from queue)
                    message = messageQueue.take();
                    System.out.println("Master: " + message);
                    String jobType = userInput.readLine();
                    if (jobType != null && jobType.equals("exit")) {
                        writeToServer.println(jobType);
                        break;
                    }
                    System.out.println("Client: Sending Job Type: " + jobType + " to Master");
                    writeToServer.println(jobType);

                    // Read dispatch confirmation (from queue)
                    message = messageQueue.take();
                    System.out.println("Master: " + message);
                } catch (InterruptedException e) {
                    System.out.println("Client: Interrupted while waiting for message");
                    break;
                }
                
                // Ask user if they want to submit another job
                System.out.println("Client: Press Enter to submit another job, or type 'exit' to quit");
                String continueChoice = userInput.readLine();
                if (continueChoice != null && continueChoice.equals("exit")) {
                    writeToServer.println("exit");
                    System.out.println("Client: Exiting...");
                    break;
                } else {
                    // Send "continue" to signal we want another job
                    writeToServer.println("continue");
                }
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