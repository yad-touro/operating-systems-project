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

    // constructor
    public MasterClientThreads(ServerSocket serverSocket, int id, String threadName, ArrayList<Job> jobList) {
        this.serverSocket = serverSocket;
        this.threadId = id;
        this.threadName = threadName;
        this.jobList = jobList;
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


            String requestString = "default String";


            do {

                System.out.println(requestString + " received by listener: " + this.threadId + " " + this.threadName);
                clientResponseWriter.println("Hello " + this.threadName);

                clientResponseWriter.println("Job ID: ");
                currentJobID = clientRequestReader.readLine();
                System.out.println(currentJobID + " received");

                clientResponseWriter.println("Job Type (A or B): ");
                currentJobType = clientRequestReader.readLine();
                System.out.println(currentJobType + " received");

                jobList.add(new Job(currentJobID, currentJobType, threadId));

                System.out.println("Dispatching...");
                clientResponseWriter.println("Dispatching Job... Enter \"exit\" to exit from Progam");



            } while(!(requestString.equals("exit")));

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