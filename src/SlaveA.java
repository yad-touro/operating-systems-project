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

public class SlaveA extends Thread{

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
    ArrayList<Thread> listOfJobs;

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
        args = new String[] {"127.0.0.1", "30122"};

        if (args.length != 2) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
                Socket slaveASocket = new Socket(hostName, portNumber);
                PrintWriter writeToServer = // stream to write text requests to server
                        new PrintWriter(slaveASocket.getOutputStream(), true);
                BufferedReader readFromServer = // stream to read text response from server
                        new BufferedReader(
                                new InputStreamReader(slaveASocket.getInputStream()));

        ) {
            String serverInputSendToSlave;
            while ((serverInputSendToSlave = readFromServer.readLine()) != null) {
                writeToServer.println(serverInputSendToSlave);
                System.out.println("echo: " + readFromServer.readLine());
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


        // pseudoCode

            // needs code that will sort of behave similarly to a Client.
            // But without allowing to take input from the console/terminal.
            // And only accepts input from the Master.

            // needs a method to process job type A
                // "sleep" for 2 seconds
                // return, end of method, tell Master job is done

            // needs a method to process Job type B
                // "sleep" for 10 seconds
                // return, end of method, tell Master job is done




}