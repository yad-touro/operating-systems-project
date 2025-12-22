/**
 * The master will calculate, based on the current load, whether it is more efficient to assign the job to the
 * slave that is optimized to perform it, or the slave that is not optimized to perform it, and assign it. The
 * master does NOT need to perform this calculation based on the current progress of any job, it can assume
 * that any job that is in progress will require the full time to complete.
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;

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
public class MasterClientThreads extends Thread{

    private ServerSocket serverSocket = null;
    private int threadId;
    private String threadName;
    private String currentJobID;
    private String currentJobType;
    ArrayList<Job> jobList;
    ArrayList<PrintWriter> clientWriters;

    // constructor
    public MasterClientThreads(ServerSocket serverSocket, int id, String threadName, 
                               ArrayList<Job> jobList, ArrayList<PrintWriter> clientWriters) {
        this.serverSocket = serverSocket;
        this.threadId = id;
        this.threadName = threadName;
        this.jobList = jobList;
        this.clientWriters = clientWriters;
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

            // Store client PrintWriter for notifications
            synchronized (clientWriters) {
                while (clientWriters.size() <= threadId) {
                    clientWriters.add(null);
                }
                clientWriters.set(threadId, clientResponseWriter);
            }
            
            System.out.println("Master: Client " + threadId + " connected");

            String requestString = "default String";

            do {
                System.out.println("Master: Waiting for job from Client " + threadId);
                clientResponseWriter.println("Hello " + this.threadName);

                clientResponseWriter.println("Job ID: ");
                currentJobID = clientRequestReader.readLine();
                System.out.println("Master: Received Job ID: " + currentJobID + " from Client " + threadId);

                clientResponseWriter.println("Job Type (A or B): ");
                currentJobType = clientRequestReader.readLine();
                System.out.println("Master: Received Job Type: " + currentJobType + " from Client " + threadId);

                // Add job to queue with synchronization
                synchronized (jobList) {
                    Job newJob = new Job(currentJobID, currentJobType, threadId);
                    jobList.add(newJob);
                    System.out.println("Master: Added job " + currentJobType + ":" + currentJobID + 
                                     " to job queue (from Client " + threadId + ")");
                }

                System.out.println("Master: Job " + currentJobType + ":" + currentJobID + " queued for dispatch");
                clientResponseWriter.println("Dispatching Job... Enter \"exit\" to exit from Program");

                // Read response from client (could be "exit" or anything to continue)
                requestString = clientRequestReader.readLine();
                if (requestString == null || requestString.equals("exit")) {
                    break;
                }
                // If not "exit", continue loop to accept another job

            } while(true);

            if (requestString.equals("exit")) {
                System.out.println("Client responds: " + requestString
                        + "\n Master is is terminating connection with Client " + threadId);
                clientResponseWriter.println("Master is terminating connection with Client " + threadId);
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

    // getter methods
    public int getThreadId() {
        return this.threadId;
    }

    public String getThreadName() {
        return this.threadName;
    }

    public String getCurrentJobID() {
        return this.currentJobID;
    }

    public String getCurrentJobType() {
        return this.currentJobType;
    }



} // end of class