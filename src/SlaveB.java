import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * There will be two different types of “slaves”, Slave-A and Slave-B. You can implement these as two
 * different Java applications or as one Java application that is set to be type A or B with a command line
 * argument. There will be two different types of “jobs”, A and B.

 * Slave-A is optimized to perform jobs of type A
 * and Slave-B is optimized to perform jobs of type B,
 * but both slave types can do the job for which they aren’t optimized, it just takes them longer.

 * You will simulate this by having a slave sleep for 2 seconds
 * for its optimal job, and 10 seconds for its non-optimal job.
 * When a slave receives a job, it should “work” on it by sleeping. When the slave is finished,
 * it alerts the master that the job is complete, and the master alerts the correct client that the job is complete.
 */

public class SlaveB extends Thread {

    // NOTE: most things that apply here also will apply for SlaveB
    //       no need to repeat the same ideas in both.
    /*
        We will need to discuss if we'd rather just create two Slave Classes or use only one that will take
        An input of sort, whether through the constructor or the command line.
        Either manner will work.

        The current thought is that having two Slave classes will be easier and less clutter in code,
        and more readable code.

        Also Note:
        We may need to add functionality according to Producer and Consumer dynamics, which probably
        requires a "circular" array of sorts.

        Mutual Exclusion MUST be implemented in the Slave Classes.
        Most likely try to implement either Dekker's or Peterson's algorithm.
        We may try Lamport's Bakery algorithm, but it may be harder.
     */

    // variables
    static String slaveType = "B";
    static final int MAX_JOBS = 6;// This will be used to check if the Slave is full or not
    static ArrayList<Job> uncompletedJobs = new ArrayList<>();
    static ArrayList<Job> completedJobs = new ArrayList<>();

    /*
        We may need to create a main method, as it seems that the Slaves must tell the Master
        that they have completed the task. This is because you do not want the Slaves to be sharing
        the same terminal as the Master, as this will clutter who's saying what to whom.
    */

    // In other words, we need to create a similar structure to how client talks to Server,
    // but WITHOUT the ability to take in input from the user.

    // main method
    public static void main(String[] args) {

        // Hardcode in IP and Port here if required
        args = new String[] {"127.0.0.1", "30122", "30123"};

        if (args.length != 3) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int incomingPortNumber = Integer.parseInt(args[1]);
        int outgoingPortNumber = Integer.parseInt(args[2]);

        try (
                Socket slaveBIncomingSocket = new Socket(hostName, incomingPortNumber);
                Socket slaveBOutgoingSocket = new Socket(hostName, outgoingPortNumber);

                PrintWriter writeToMasterWhenReceivingNewJobs = // stream to write text requests to server
                        new PrintWriter(slaveBIncomingSocket.getOutputStream(), true);
                BufferedReader readFromMasterWhenReceivingNewJobs = // stream to read text response from server
                        new BufferedReader(new InputStreamReader(slaveBIncomingSocket.getInputStream()));

                PrintWriter writeToMasterWhenSendingCompletedJobs =
                        new PrintWriter(slaveBOutgoingSocket.getOutputStream(), true);
                BufferedReader readFromMasterWhenSendingCompletedJobs =
                        new BufferedReader(new InputStreamReader(slaveBOutgoingSocket.getInputStream()));
        ) {

            System.out.println("Attempting to connect to Master");
            writeToMasterWhenReceivingNewJobs.println(slaveType);
            System.out.println(readFromMasterWhenReceivingNewJobs.readLine());


            while (slaveBIncomingSocket != null) {

                if (!completedJobs.isEmpty()) {
                    System.out.println("***************");
                    System.out.println("Notifying Master of Job completion.");
                    writeToMasterWhenSendingCompletedJobs.println("Jobs are complete");
                    System.out.println("Sending Job Details to Master");
                    writeToMasterWhenSendingCompletedJobs.println(completedJobs.getFirst().getJobID());
                    writeToMasterWhenSendingCompletedJobs.println(completedJobs.getFirst().getJobType());
                    writeToMasterWhenSendingCompletedJobs.println(completedJobs.getFirst().getClientNumber());
                    System.out.println("Removing Job with ID: " + completedJobs.getFirst().getJobID());
                    synchronized (completedJobs) {
                        completedJobs.removeFirst();
                    }
                    System.out.println("***************");
                }

                System.out.println(readFromMasterWhenReceivingNewJobs.readLine());
                writeToMasterWhenReceivingNewJobs.println(isFull());
                System.out.println("Master Responded: " + readFromMasterWhenReceivingNewJobs.readLine());
                String clientNumber = readFromMasterWhenReceivingNewJobs.readLine();
                String jobId = readFromMasterWhenReceivingNewJobs.readLine();
                String jobType = readFromMasterWhenReceivingNewJobs.readLine();
                boolean jobStatus = Boolean.parseBoolean(readFromMasterWhenReceivingNewJobs.readLine());
                synchronized (uncompletedJobs) {
                    uncompletedJobs.add(new Job(jobId, jobType, Integer.parseInt(clientNumber), jobStatus));
                }
                System.out.println("Job " + jobId + " accepted and entered queue to be processed");


                while (!uncompletedJobs.isEmpty()) {
                    // simulate "work"
                    doJob doJob = new doJob(uncompletedJobs.getFirst(), completedJobs);

                    synchronized (uncompletedJobs) {
                        uncompletedJobs.removeFirst();
                    }
                    doJob.start();

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

    }  // end main


    public static boolean isFull() {
        return uncompletedJobs.size() == MAX_JOBS;
    }

    private static class doJob extends Thread {
        Job jobToPerform;
        ArrayList<Job> completedJobs;

        // constructor
        public doJob(Job jobToPerform, ArrayList<Job> completedJobs) {
            this.jobToPerform = jobToPerform;
            this.completedJobs = completedJobs;
        }

        @Override
        public void run() {
            if (jobToPerform.getJobType().equals("B")) {
                System.out.println("Putting to sleep for 2 seconds");
                try {
                    super.sleep(2000);  // puts the Thread that calls sleep, well, asleep
                } catch (InterruptedException e) {  // and allows the other thread to execute
                    throw new RuntimeException(e);
                }
            }
            else if (jobToPerform.getJobType().equals("A")) {
                System.out.println("Putting to sleep for 10 seconds");
                try {
                    super.sleep(10000);  // puts the Thread that calls sleep, well, asleep
                } catch (InterruptedException e) {  // and allows the other thread to execute
                    throw new RuntimeException(e);
                }
            }

            synchronized (completedJobs) {
                System.out.println("Adding to Completed Job List.");
                this.completedJobs.add(jobToPerform);  // once the is helper thread is done, add Job to a completed list.
            }

        }
    }  // end of private class doJob


}