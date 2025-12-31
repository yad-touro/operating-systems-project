/*
    Peter (Yosef) Ross
    Touro ID: T00563986

    Paul (Shlomo) Ross
    Touro ID: T00564089

    Joseph Guindi
    Touro ID: T00553821

    Yehoshua Dusowitz
    Touro ID:

 */

/*
    This class is used in order to send Jobs that are waiting to be processed from
    Master to a Slave. Due to the manner of how Sockets and Ports work, we are only
    able to keep track of one direction (either "incoming data" or "outgoing data")
    of communication for a given thread; otherwise, if we chose to make a single
    thread keep track of both directions, then incoming data would have to wait
    for outgoing data to finish (and vice versa) to proceed. In our example, we
    would not be able to add an "uncompleted Job" to a Slave's list of uncompletedJobs
    while the Slave wants to inform Master of a Job that they have finished processing.

    The little bit of Mutual Exclusion we use is by invoking the "synchronized()"
    method making sure that only one thread adds or removes items from a shared resource,
    for example the ArrayList "uncompletedJobs".

 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MasterToSlaveThreads extends Thread{

    private ServerSocket serverOutgoingSocket = null;
    private int threadId;
    private String threadName;
    private ArrayList<Job> uncompletedJobs;

    public String slaveType;

    // the following two booleans will keep track of if a Slave is too full
    // and can not be given an additional Job to enqueue into their "list of
    // uncompleted jobs". The values are initialized as false and are changed
    // through a series of PrintWriters and BufferReaders between this Class
    // the SlaveA and SLaveB Classes
    public static boolean slaveAisFull = false;
    public static boolean slaveBisFull = false;

    // constructor
    public MasterToSlaveThreads(ServerSocket serverOutgoingSocketSocket, int id,
                                String threadName, ArrayList<Job> listOfJobs) {
        this.serverOutgoingSocket = serverOutgoingSocketSocket;
        this.threadId = id;
        this.threadName = threadName;
        this.uncompletedJobs = listOfJobs;
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
        // In our project, slaveResponseWriterWhenSendingNewJobs (a PrintWriter object) shares
        // a virtual page with readFromMasterWhenReceivingNewJobs (a BufferReader object) found
        // in SlaveA (and SlaveB). slaveRequestReaderWhenSendingNewJobs (a BufferReader) shares
        // a virtual page with writeToMasterWhenReceivingNewJobs (a PrintWriter) found in SlaveA
        // (and SlaveB).
        try (Socket slaveOutgoingSocket = serverOutgoingSocket.accept();
             PrintWriter slaveResponseWriterWhenSendingNewJobs =
                     new PrintWriter(slaveOutgoingSocket.getOutputStream(), true);
             BufferedReader slaveRequestReaderWhenSendingNewJobs =
                     new BufferedReader(new InputStreamReader(slaveOutgoingSocket.getInputStream()));
        ) {

            System.out.println("Connecting to Slave...");
            slaveType = slaveRequestReaderWhenSendingNewJobs.readLine();
            System.out.println("Slave connected on port " + this.serverOutgoingSocket.getLocalPort() + " is Slave " + slaveType);
            slaveResponseWriterWhenSendingNewJobs.println("Successfully Connected to Master");


            while(slaveOutgoingSocket != null) {

//                System.out.println("checking if there are Jobs to delegate");  // used for debugging

                if (!uncompletedJobs.isEmpty() && slaveType.equals("A")) {

//                    System.out.println("Entered this part"); // used for debugging
//                    System.out.println(uncompletedJobs.getFirst().getJobType()); // used for debugging
//                    System.out.println(uncompletedJobs.getFirst().getJobID()); // used for debugging
//                    System.out.println(uncompletedJobs.getFirst().getJobType().equals("A")); // used for debugging

                    if (uncompletedJobs.getFirst().getJobType().equals("A")) {
                        synchronized (uncompletedJobs) {
                            System.out.println("***************");
                            System.out.println("Entered Synchronized block");
                            slaveResponseWriterWhenSendingNewJobs.println("Attempting Job Delegation");
                            // Asking Slave if they are too full/too busy
                            slaveResponseWriterWhenSendingNewJobs.println("Are you full?");
                            // Slave answers if they are full
                            slaveAisFull = Boolean.parseBoolean(slaveRequestReaderWhenSendingNewJobs.readLine());
                            System.out.println("Slave " + slaveType + " responded isFull: " + slaveAisFull);
                            if (!slaveAisFull) {
                                // Delegating Jobs to Slave
                                System.out.println("Delegating to Slave " + slaveType);
                                // send over the values of clientNumber, jobId, jobType and jobStatus to Slave
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getClientNumber());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobID());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobType());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getCompleted());
                                // remove the above job from the uncompletedJobList
                                uncompletedJobs.removeFirst();
                            }
                            else {
                                System.out.println("Cannot delegate task to Slave " + slaveType);
                            }
                            System.out.println("***************");
                        }
                    }
                    else if (uncompletedJobs.getFirst().getJobType().equals("B") && slaveBisFull) {
                        synchronized (uncompletedJobs) {
                            System.out.println("***************");
                            slaveResponseWriterWhenSendingNewJobs.println("Attempting Job Delegation");
                            // Asking Slave if they are too full/too busy
                            slaveResponseWriterWhenSendingNewJobs.println("Are you full?");
                            // Slave answers if they are full
                            slaveAisFull = Boolean.parseBoolean(slaveRequestReaderWhenSendingNewJobs.readLine());
                            System.out.println("Slave " + slaveType + " responded isFull: " + slaveBisFull);
                            if (!slaveAisFull) {
                                // Delegating Jobs to Slave
                                System.out.println("Delegating to Slave " + slaveType);
                                // send over the values of clientNumber, jobId, jobType and jobStatus to Slave
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getClientNumber());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobID());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobType());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getCompleted());
                                uncompletedJobs.removeFirst();
                            }
                            else {
                                System.out.println("Cannot delegate task to Slave " + slaveType);
                            }
                            System.out.println("***************");
                        }
                    }

                }

                else if (!uncompletedJobs.isEmpty() && slaveType.equals("B")) {
                    if (uncompletedJobs.getFirst().getJobType().equals("B")) {
                        synchronized (uncompletedJobs) {
                            System.out.println("***************");
                            slaveResponseWriterWhenSendingNewJobs.println("Attempting Job Delegation");
                            // Asking Slave if they are too full/too busy
                            slaveResponseWriterWhenSendingNewJobs.println("Are you full?");
                            // Slave answers if they are full
                            slaveBisFull = Boolean.parseBoolean(slaveRequestReaderWhenSendingNewJobs.readLine());
                            System.out.println("Slave " + slaveType + " responded isFull: " + slaveBisFull);
                            if (!slaveBisFull) {
                                // Delegating Jobs to Slave
                                System.out.println("Delegating to Slave " + slaveType);
                                // send over the values of clientNumber, jobId, jobType and jobStatus to Slave
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getClientNumber());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobID());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobType());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getCompleted());
                                uncompletedJobs.removeFirst();
                            }
                            else {
                                System.out.println("Cannot delegate task to Slave " + slaveType);
                            }
                            System.out.println("***************");
                        }
                    }
                    else if (uncompletedJobs.getFirst().getJobType().equals("A") && slaveAisFull) {
                        synchronized (uncompletedJobs) {
                            System.out.println("***************");
                            slaveResponseWriterWhenSendingNewJobs.println("Attempting Job Delegation");
                            // Asking Slave if they are too full/too busy
                            slaveResponseWriterWhenSendingNewJobs.println("Are you full?");
                            // Slave answers if they are full
                            slaveBisFull = Boolean.parseBoolean(slaveRequestReaderWhenSendingNewJobs.readLine());
                            System.out.println("Slave " + slaveType + " responded isFull: " + slaveBisFull);
                            if (!slaveBisFull) {
                                // Delegating Jobs to Slave
                                System.out.println("Delegating to Slave " + slaveType);
                                // send over the values of clientNumber, jobId, jobType and jobStatus to Slave
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getClientNumber());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobID());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getJobType());
                                slaveResponseWriterWhenSendingNewJobs.println(uncompletedJobs.getFirst().getCompleted());
                                uncompletedJobs.removeFirst();

                            }
                            else {
                                System.out.println("Cannot delegate task to Slave " + slaveType);
                            }
                            System.out.println("***************");
                        }
                    }

                }

                try {
                    sleep(10);  // puts the Thread that calls sleep, well, asleep
                } catch (InterruptedException e) {  // and allows the other thread to execute
                    throw new RuntimeException(e);
                }



            }
        } catch (IOException e) {

            System.out.println(
                    "Exception caught when trying to listen on port "
                            + serverOutgoingSocket.getLocalPort()
                            + " or listening for a connection"
            );

            System.out.println(e.getMessage());

        }  // end catch


    }  // end of run

    public String getThreadName() {
        return this.threadName;
    }


}

