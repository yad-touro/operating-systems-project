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

import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * Project Directions as set by the Professor:
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

public class SlaveA extends Thread{

    // NOTE: most things that apply here also will apply for SlaveB
    //       no need to repeat the same ideas in both. Go read SlaveB
    //      for explanations of unexplained pieces of code

    // variables
    static String slaveType = "A";
    static final int MAX_JOBS = 4;// This will be used to check if the Slave is full or not
    static ArrayList<Job> uncompletedJobs = new ArrayList<>();
    static ArrayList<Job> completedJobs = new ArrayList<>();

    /*
        We need to create a main method, as it seems that the Slaves must tell the Master
        that they have completed the task. This is because you do not want the Slaves to be sharing
        the same terminal as the Master, as this will clutter who's saying what to whom. Plus the
        project requires that the slaves have their own working terminal and should not be an
        object/method that takes in a Job to process.
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
                Socket slaveAIncomingSocket = new Socket(hostName, incomingPortNumber);
                Socket slaveAOutgoingSocket = new Socket(hostName, outgoingPortNumber);

                PrintWriter writeToMasterWhenReceivingNewJobs = // stream to write text requests to server
                        new PrintWriter(slaveAIncomingSocket.getOutputStream(), true);
                BufferedReader readFromMasterWhenReceivingNewJobs = // stream to read text response from server
                        new BufferedReader(new InputStreamReader(slaveAIncomingSocket.getInputStream()));

                PrintWriter writeToMasterWhenSendingCompletedJobs =
                        new PrintWriter(slaveAOutgoingSocket.getOutputStream(), true);
                BufferedReader readFromMasterWhenSendingCompletedJobs =
                        new BufferedReader(new InputStreamReader(slaveAOutgoingSocket.getInputStream()));

        ) {

            System.out.println("Attempting to connect to Master");
            writeToMasterWhenReceivingNewJobs.println(slaveType);
            System.out.println(readFromMasterWhenReceivingNewJobs.readLine());


            while (slaveAIncomingSocket != null) {

                if (!completedJobs.isEmpty()) {
                    System.out.println("***************");
                    System.out.println("Notifying Master of Job completion.");
                    writeToMasterWhenSendingCompletedJobs.println("Jobs are complete");
                    System.out.println("Sending Job Details to Master");
                    writeToMasterWhenSendingCompletedJobs.println(completedJobs.getFirst().getJobID());
                    writeToMasterWhenSendingCompletedJobs.println(completedJobs.getFirst().getJobType());
                    writeToMasterWhenSendingCompletedJobs.println(completedJobs.getFirst().getClientNumber());
                    writeToMasterWhenSendingCompletedJobs.println(completedJobs.getFirst().getCompleted());
                    System.out.println("Removing Job with ID: " + completedJobs.getFirst().getJobID());
                    synchronized (completedJobs) {
                        completedJobs.removeFirst();
                    }
                    System.out.println("***************");
                }

                // Master Asks Slave if they are too busy
                System.out.println(readFromMasterWhenReceivingNewJobs.readLine());
                System.out.println("Master Responded: " + readFromMasterWhenReceivingNewJobs.readLine());
                // Answering Master if Slave is full/too busy.
                System.out.println("Answering Master isFull: " + isFull());
                writeToMasterWhenReceivingNewJobs.println(isFull());
                if(!isFull()) {
                    // Master is Delegating Job to Slave
                    // read in the values of clientNumber, jobId, jobType and jobStatus which Master will be sending over
                    String clientNumber = readFromMasterWhenReceivingNewJobs.readLine();
                    String jobId = readFromMasterWhenReceivingNewJobs.readLine();
                    String jobType = readFromMasterWhenReceivingNewJobs.readLine();
                    boolean jobStatus = Boolean.parseBoolean(readFromMasterWhenReceivingNewJobs.readLine());
                    // Add Job to Slave's exclusive/individual uncompleted Jobs list
                    synchronized (uncompletedJobs) {
                        uncompletedJobs.add(new Job(jobId, jobType, Integer.parseInt(clientNumber), jobStatus));
                    }
                    System.out.println("Job " + jobId + " accepted and entered queue to be processed");
                } else {
                    System.out.println("System is too busy...");
                }


                if (!uncompletedJobs.isEmpty()) {
                    // simulate "work"
                    doJob doJob = new doJob(uncompletedJobs, completedJobs);

                    doJob.start();

                }

            }

            try {
                sleep(10);  // puts the Thread that calls sleep, well, asleep
            } catch (InterruptedException e) {  // and allows the other thread to execute
                throw new RuntimeException(e);
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
        return uncompletedJobs.size() >= MAX_JOBS;
    }

    private static class doJob extends Thread {
        ArrayList<Job> uncompletedJobs;
        ArrayList<Job> completedJobs;

        // constructor
        public doJob(ArrayList<Job> uncompletedJobs, ArrayList<Job> completedJobs) {
            this.uncompletedJobs = uncompletedJobs;
            this.completedJobs = completedJobs;
        }

        @Override
        public void run() {
            if (uncompletedJobs.getFirst().getJobType().equals("A")) {
                System.out.println("Putting Job " + uncompletedJobs.getFirst().getJobID() + " to sleep for 2 seconds");
                try {
                    this.sleep(2000);  // puts the Thread that calls sleep, well, asleep
                    synchronized (completedJobs) {
                        uncompletedJobs.getFirst().setCompleted(true);
                        System.out.println("Adding to Completed Job List.");
                        this.completedJobs.add(uncompletedJobs.getFirst());  // once the is helper thread is done, add Job to a completed list.
                        uncompletedJobs.removeFirst();
                    }

                } catch (InterruptedException e) {  // and allows the other thread to execute
                    throw new RuntimeException(e);
                }
            }
            else if (uncompletedJobs.getFirst().getJobType().equals("B")) {
                System.out.println("Putting  Job " + uncompletedJobs.getFirst().getJobID() + " to sleep for 10 seconds");
                try {
                    this.sleep(10000);  // puts the Thread that calls sleep, well, asleep
                    synchronized (completedJobs) {
                        uncompletedJobs.getFirst().setCompleted(true);
                        System.out.println("Adding to Completed Job List.");
                        this.completedJobs.add(uncompletedJobs.getFirst());  // once the is helper thread is done, add Job to a completed list.
                        uncompletedJobs.removeFirst();
                    }

                } catch (InterruptedException e) {  // and allows the other thread to execute
                    throw new RuntimeException(e);
                }
            }

        }
    }  // end of private class doJob

}