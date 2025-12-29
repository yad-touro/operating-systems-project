/**
 * The master will calculate, based on the current load, whether it is more efficient to assign the job to the
 * slave that is optimized to perform it, or the slave that is not optimized to perform it, and assign it. The
 * master does NOT need to perform this calculation based on the current progress of any job, it can assume
 * that any job that is in progress will require the full time to complete.
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;


public class Master extends Thread {


    public static void main(String[] args) {

        ArrayList<Job> listOfJobs = new ArrayList<>(); // Uncompleted Jobs waiting to be dispatched to a Slave
        ArrayList<Job> listOfCompletedJobs = new ArrayList<>(); // Jobs that have been completed by a Slave

        // Hardcode port number if necessary
        args = new String[] { "30121" , "30122", "30123", "30124" };

        if (args.length != 4) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }

        int clientOutgoingPortNumber = Integer.parseInt(args[0]);
        int slaveOutgoingPortNumber = Integer.parseInt(args[1]);
        int slaveIncomingPortNumber = Integer.parseInt(args[2]);
        int clientIncomingPortNumber = Integer.parseInt(args[3]);

        // this will allow the total amount of threads or clients to connect
        final int MAX_CLIENT_THREADS = 4;  // threads will be numbered 0 -> MAX_THREADS - 1, for now it's 4, but if you want, you can change for more.
        final int MAX_SLAVE_THREADS = 2;

        try (ServerSocket serverClientMasterSocket = new ServerSocket(clientOutgoingPortNumber);
             ServerSocket serverMasterClientSocket = new ServerSocket(clientIncomingPortNumber);
             ServerSocket serverSlaveOutgoingSocket = new ServerSocket(slaveOutgoingPortNumber);
             ServerSocket serverSlaveIncomingSocket = new ServerSocket(slaveIncomingPortNumber)
        ) {

            // an ArrayList to keep track of all threads
            ArrayList<Thread> listOfClientToMasterThreads = new ArrayList<Thread>();
//            ArrayList<MasterToClientThreads> listOfMasterToClientThreads = new ArrayList<>();
            ArrayList<MasterToSlaveThreads> listOfMasterToSlaveThreads = new ArrayList<MasterToSlaveThreads>();
            ArrayList<SlaveToMasterThreads> listOfSlaveToMasterThreads = new ArrayList<>();

            // creation of threads to connect Clients to Master. These Threads will be monitoring the data and
            // requests coming from Clients and sending to Master (i.e. sends the uncompleted Jobs to Master to be
            // delegated soon)
            for (int i = 0; i < MAX_CLIENT_THREADS; i++) {
                listOfClientToMasterThreads.add(new Thread(new ClientToMasterThreads(serverClientMasterSocket,
                                                                                    i,
                                                                        "Client Thread " + i,
                                                                                    listOfJobs,
                                                                                    listOfCompletedJobs)));
//                listOfMasterToClientThreads.add(new MasterToClientThreads(serverMasterClientSocket,
//                                                                            i,
//                                                                "Client Thread " + i,
//                                                                            listOfCompletedJobs));
            }

            // creation of threads to connect Master to Clients. These Threads will be monitoring the data
            // to tell Clients that Jobs are complete.
//            for (int i = 0; i < MAX_CLIENT_THREADS; i++) {
//                listOfMasterToClientThreads.add(new MasterToClientThreads(serverMasterClientSocket,
//                                                                          i,
//                                                               "Client Thread " + 1,
//                                                                          listOfCompletedJobs));
//            }

            // creation of threads to connect Master and Slaves. These Threads will be monitoring the data and
            // requests coming from Master and sending to Slaves (i.e. when a job needs to be processed by a Slave)
            for (int i = 0; i < MAX_SLAVE_THREADS; i++) {
                listOfMasterToSlaveThreads.add(new MasterToSlaveThreads(serverSlaveOutgoingSocket,
                                                                        i,
                                                                "Slave Thread " + i,
                                                                        listOfJobs));
                listOfSlaveToMasterThreads.add(new SlaveToMasterThreads(serverSlaveIncomingSocket,
                        i,
                        "Slave Thread " + i,
                        listOfCompletedJobs));

            }

            // creation of threads to connect Master and Slaves. These Threads will be monitoring the data and
            // requests coming from Slaves and sending to Master (i.e. when a Job has been completed by a Slave)
//            for (int i = 0; i < MAX_SLAVE_THREADS; i++) {
//                listOfSlaveToMasterThreads.add(new SlaveToMasterThreads(serverSlaveIncomingSocket,
//                                                                        i,
//                                                             "Slave Thread " + i,
//                                                                        listOfCompletedJobs));
//            }


            // any "start()" calls need to be placed here
            // starting threads
            for (Thread t : listOfClientToMasterThreads) {
                t.start();
            }


            for (Thread t : listOfMasterToSlaveThreads) {
                t.start();
            }

            for (Thread t : listOfSlaveToMasterThreads) {
                t.start();
            }

//            for (Thread t : listOfMasterToClientThreads) {
//                t.start();
//            }


            // any "join()" calls need to be placed here.
            for (Thread t : listOfClientToMasterThreads) {
                try {
                    t.join();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            // join
            for (Thread t : listOfMasterToSlaveThreads) {
                try {
                    t.join();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            for (Thread t: listOfSlaveToMasterThreads) {
                try {
                    t.join();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

//            for (Thread t: listOfMasterToClientThreads) {
//                try {
//                    t.join();
//                } catch (InterruptedException e){
//                    e.printStackTrace();
//                }
//            }

            // joining threads so that they will be more or less in parallel


        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + clientOutgoingPortNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }  // end catch


    }  // end of main

}  // end of class