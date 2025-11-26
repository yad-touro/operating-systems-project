/*
    NOTE:
        This is most likely the class where we'll need to implement the
        Mutual Exclusion of threads between Master and Slaves.

    Also Note:
        It may not actually be necessary to have this class.
        We need to discuss where Mutual Exclusion is actually needed.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MasterSlaveThreads implements Runnable{

    private ServerSocket serverSocket = null;
    int threadId;
    String threadName;

    // constructor
    public MasterSlaveThreads(ServerSocket serverSocket, int id, String threadName) {
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

        try (Socket slaveSocket = serverSocket.accept();

             PrintWriter slaveResponseWriter = new PrintWriter(slaveSocket.getOutputStream(), true);
             BufferedReader slaveRequestReader = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {
            String requestString;


            while((requestString = slaveRequestReader.readLine()) != null) {
                System.out.println(requestString + " received by listener: " + this.threadId + " " + this.threadName);
                slaveResponseWriter.println();
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
}
