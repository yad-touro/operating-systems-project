/**
 * The master will calculate, based on the current load, whether it is more efficient to assign the job to the
 * slave that is optimized to perform it, or the slave that is not optimized to perform it, and assign it. The
 * master does NOT need to perform this calculation based on the current progress of any job, it can assume
 * that any job that is in progress will require the full time to complete.
 */

import java.io.*;
import java.net.*;

/**
 * This class is what the Master will use to implement "n" amount of threads.
 * Each thread will need an ID (0 --> n), name, and a ServerSocket reference.
 */

/*
    NOTE:
        This is most likely the class where we'll need to implement the
        Mutual Exclusion of threads between Master and Client.

        HOWEVER, we need to speak about where to actually implement
 */
public class MasterClientThreads implements Runnable{

    private ServerSocket serverSocket = null;
    int threadId;
    String threadName;

    // constructor
    public MasterClientThreads(ServerSocket serverSocket, int id, String threadName) {
        this.serverSocket = serverSocket;
        this.threadId = id;
        this.threadName = threadName;
    }

    @Override
    public void run() {  // NOTE: !!!MUST PUT EXPLANATION OF CODE EVENTUALLY!!!

        /*
            Also, we will need to start naming variables more accurately.
            However, we need to see how the server and clients will respond
            to each other.
        */

        try (Socket clientSocket = serverSocket.accept();

             PrintWriter clientResponseWriter = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader clientRequestReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String requestString;


            while((requestString = clientRequestReader.readLine()) != null) {
                System.out.println(requestString + " received by listener: " + this.threadId + " " + this.threadName);
                clientResponseWriter.println();
            }

        } catch (IOException e) {

            System.out.println(
                    "Exception caught when trying to listen on port "
                            + serverSocket.getLocalPort()
                            + " or listening for a connection"
            );

            System.out.println(e.getMessage());

        }  // end catch


    }  // end of run

} // end of class