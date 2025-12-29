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

public class ClientToMasterThreads extends Thread{

    private ServerSocket serverSocket = null;
    private int threadId;
    private String threadName;
    private String currentJobID;
    private String currentJobType;
    ArrayList<Job> jobList;
    ArrayList<Job> completedJobs;

    // constructor
    public ClientToMasterThreads(ServerSocket serverSocket, int id, String threadName, ArrayList<Job> jobList, ArrayList<Job> completedJobs) {
        this.serverSocket = serverSocket;
        this.threadId = id;
        this.threadName = threadName;
        this.jobList = jobList;
        this.completedJobs = completedJobs;
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
             BufferedReader clientRequestReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {

            // this part of the code is the initial connection between Client and Master.
            System.out.println("Connecting to Client " + this.threadId);
            System.out.println(this.threadName + " connected on port " + clientSocket.getLocalPort());
            clientResponseWriter.println("Successfully Connected to Master");
            String requestString = "";

            // this do while loop will ask  the client for input of Jobs and the job type.
            // Since we're only handling type A and B, there is some input handling that will
            // let client know that they did not input valid types.
            do {
                System.out.println("Asking " + threadName + " For Job ID...");
                clientResponseWriter.println("Job ID: ");
                currentJobID = clientRequestReader.readLine();
                if (currentJobID.equals("exit")) break;
                System.out.println(currentJobID + " received");

                System.out.println("Asking " + threadName + "For Job Type...");
                clientResponseWriter.println("Job Type (A or B): ");
                currentJobType = clientRequestReader.readLine();
                System.out.println(currentJobType + " received");

                if (!(currentJobType.equals("A") || currentJobType.equals("B"))) {
                    clientResponseWriter.println("Please input type \"A\" or \"B\"");
                    clientResponseWriter.println("Enter your next job");
                }

                else {
                    synchronized (jobList) {
                        jobList.add(new Job(currentJobID, currentJobType, threadId, false));
                    }

                    System.out.println("JobId: " + jobList.getFirst().getJobID() + " JobType: " + jobList.getFirst().getJobType());

                    clientResponseWriter.println("Job added to list");

                    System.out.println("Dispatching...");
                    clientResponseWriter.println("Dispatching Job... Enter \"exit\" to exit from Program");
                }

//                if (!completedJobs.isEmpty()) {
//                    if (completedJobs.getFirst().getClientNumber() == threadId) {
//                        clientResponseWriter.println("A Job has completed.");
//                        clientResponseWriter.println(completedJobs.getFirst().getJobID() + "has completed.");
//                    }
//                }


            } while(clientSocket != null);

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