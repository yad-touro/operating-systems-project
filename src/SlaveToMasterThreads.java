
/*
    This class is used in order to send Jobs that have been processed/finished
    and that Slave will then inform Master of the completion of said Job. Due
    to the manner of how Sockets and Ports work, we are only able to keep track
    of one direction (either "incoming data" or "outgoing data") of communication
    for a given thread; otherwise, if we chose to make a single thread keep track
    of both directions, then incoming data would have to wait for outgoing data
    to finish (and vice versa) to proceed. In our example, we would not be able to
    add a "completed Job" to Master's list of completedJobs while the Master wants
     to give a Slave a Job that needs to be processed and worked on.

    The little bit of Mutual Exclusion we use is by invoking the "synchronized()"
    method making sure that only one thread adds or removes items from a shared resource,
    for example the ArrayList "completedJobs".

*/



import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class SlaveToMasterThreads extends Thread {
    private ServerSocket slaveToMasterSocket = null;
    private int threadId;
    private String threadName;
    private ArrayList<Job> completedJobs;

    // constructor
    public SlaveToMasterThreads(ServerSocket slaveToMasterSocket, int id, String threadName, ArrayList<Job> completedJobs) {
        this.slaveToMasterSocket = slaveToMasterSocket;
        this.threadId = id;
        this.threadName = threadName;
        this.completedJobs = completedJobs;
    }

    @Override
    public void run() {

        // Very important to understand how Sockets, PrintWriters and BufferReaders work.
        // slaveOutgoingSocket will find a designated Port (passed through the constructor
        // called in Main) on the computer in order for this Thread Class to have a secure
        // and exclusive connection to a Slave in order to do the specific Job it was given
        // (i.e. sending over Jobs that are in need of processing).
        // Every PrintWriter has a BufferReader that pairs up with it (and the two do no have
        // to belong to the same class) and they share a "virtual" page that is not printed
        // onto the consoles. PrintWriters only write on that virtual page and BufferReaders
        // only read from that virtual page.
        // In our project, writeToSlave (a PrintWriter object) shares a virtual page with
        // readFromMasterWhenSendingCompletedJobs (a BufferReader object) found in SlaveA
        // (and SlaveB). readFromSlave (a BufferReader) shares a virtual page with
        // writeToMasterWhenSendingCompletedJobs (a PrintWriter) found in SlaveA
        // (and SlaveB).
        try (Socket slaveSocket = slaveToMasterSocket.accept();
             PrintWriter writeToSlave = new PrintWriter(slaveSocket.getOutputStream(), true);
             BufferedReader readFromSlave = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))
        ) {

            while (slaveSocket != null) {

                System.out.println("***************");
                System.out.println("Checking if there are completed jobs");
                System.out.println("***************");

                if (readFromSlave.readLine().equals("Jobs are complete")) {
                    System.out.println("Inputting Completed Job to list of Completed Jobs");
                    String completedJobId = readFromSlave.readLine();
                    String completedJobType = readFromSlave.readLine();
                    int completedClientNumber = Integer.parseInt(readFromSlave.readLine());
                    boolean completedJob = Boolean.parseBoolean(readFromSlave.readLine());
                    System.out.println("***************");
                    System.out.println("Entering Completed Job: " +
                            "\nJob ID: " + completedJobId +
                            "\nJob Type: " + completedJobType +
                            "\nClient Number: " + completedClientNumber +
                             "\nJob Completed: " + completedJob);
                    synchronized (completedJobs) {
                        completedJobs.add(new Job(completedJobId, completedJobType, completedClientNumber, completedJob));
                    }
                    System.out.println("***************");
                }

//                // for debugging purposes
//                for (Job j : completedJobs) {
//                    System.out.println(j);
//                }

            }

            try {
                sleep(10);  // puts the Thread that calls sleep, well, asleep
            } catch (InterruptedException e) {  // and allows the other thread to execute
                throw new RuntimeException(e);
            }

        }
        catch (IOException e){
            System.out.println(
                    "Exception caught when trying to listen on port "
                            + slaveToMasterSocket.getLocalPort()
                            + " or listening for a connection"
            );

            System.out.println(e.getMessage());
        }
    }
}
