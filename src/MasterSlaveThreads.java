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
import java.util.ArrayList;

public class MasterSlaveThreads implements Runnable{

    private ServerSocket serverSocket = null;
    int threadId;
    String threadName;
    
    // Shared data structures - passed from Master
    private ArrayList<Job> listOfCompletedJobs;
    private ArrayList<Job> listOfJobsGivenToSlave1;
    private ArrayList<Job> listOfJobsGivenToSlave2;
    private ArrayList<PrintWriter> clientWriters;
    
    // Static arrays to store slave PrintWriters for dispatcher access
    public static PrintWriter[] slaveWriters = new PrintWriter[2];
    public static String[] slaveTypes = new String[2];
    public static Object[] slaveLocks = new Object[]{new Object(), new Object()};

    // constructor
    public MasterSlaveThreads(ServerSocket serverSocket, int id, String threadName,
                             ArrayList<Job> listOfCompletedJobs,
                             ArrayList<Job> listOfJobsGivenToSlave1,
                             ArrayList<Job> listOfJobsGivenToSlave2,
                             ArrayList<PrintWriter> clientWriters) {
        this.serverSocket = serverSocket;
        this.threadId = id;
        this.threadName = threadName;
        this.listOfCompletedJobs = listOfCompletedJobs;
        this.listOfJobsGivenToSlave1 = listOfJobsGivenToSlave1;
        this.listOfJobsGivenToSlave2 = listOfJobsGivenToSlave2;
        this.clientWriters = clientWriters;
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
            
            // Log when a slave connects
            System.out.println("Master: Slave connected on thread " + this.threadId + " (" + this.threadName + ")");
            
            // Wait for slave identification
            String identification = slaveRequestReader.readLine();
            if (identification != null && identification.startsWith("SLAVE_TYPE:")) {
                String slaveType = identification.substring(11); // Extract "A" or "B"
                slaveTypes[threadId] = slaveType;
                slaveWriters[threadId] = slaveResponseWriter;
                System.out.println("Master: Slave identified as type " + slaveType + " on thread " + threadId);
            }
            
            String requestString;
            while((requestString = slaveRequestReader.readLine()) != null) {
                // Handle completion messages
                if (requestString.startsWith("COMPLETE:")) {
                    // Parse: "COMPLETE:A:123"
                    String[] parts = requestString.split(":");
                    if (parts.length == 3) {
                        String jobType = parts[1];
                        String jobId = parts[2];
                        
                        System.out.println("Master: Received completion from Slave-" + slaveTypes[threadId] + 
                                         ": Job Type=" + jobType + ", ID=" + jobId + " is complete");
                        
                        // Find and move job from slave list to completed list
                        synchronized (slaveLocks[threadId]) {
                            ArrayList<Job> slaveJobList = (threadId == 0) ? listOfJobsGivenToSlave1 : listOfJobsGivenToSlave2;
                            Job completedJob = null;
                            for (Job job : slaveJobList) {
                                if (job.getJobID().equals(jobId) && job.getJobType().equals(jobType)) {
                                    completedJob = job;
                                    break;
                                }
                            }
                            
                            if (completedJob != null) {
                                slaveJobList.remove(completedJob);
                                synchronized (listOfCompletedJobs) {
                                    listOfCompletedJobs.add(completedJob);
                                }
                                
                                // Notify the client
                                int clientNum = completedJob.getClientNumber();
                                synchronized (clientWriters) {
                                    if (clientNum < clientWriters.size() && clientWriters.get(clientNum) != null) {
                                        PrintWriter clientWriter = clientWriters.get(clientNum);
                                        clientWriter.println("JOB_COMPLETE:" + jobType + ":" + jobId);
                                        System.out.println("Master: Notifying Client " + clientNum + 
                                                         " that job " + jobType + ":" + jobId + " is complete");
                                    }
                                }
                                
                                System.out.println("Master: Slave-" + slaveTypes[threadId] + " is now available");
                            }
                        }
                    }
                } else {
                    System.out.println("Master: Received from slave [" + this.threadName + "]: " + requestString);
                }
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
