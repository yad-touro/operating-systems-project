/**
 * The master will calculate, based on the current load, whether it is more efficient to assign the job to the
 * slave that is optimized to perform it, or the slave that is not optimized to perform it, and assign it. The
 * master does NOT need to perform this calculation based on the current progress of any job, it can assume
 * that any job that is in progress will require the full time to complete.
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;

/*
    Note that Master needs to implement some form of Mutual Exclusion.
    This is because it needs to manage incoming/outgoing "messages" (or whatever you want to call the interaction)
    between itself, and the Clients and the Slaves.
 */

public class Master {

    // needs some Global variables to keep track of which thread should be prioritized
    // and taken care of. Must discuss what variables exactly.

    int favoredClientThread;
    int favoredSlaveThread;
    ArrayList<Thread> listOfAllThreads;// to keep track of all threads currently running, this is a hypothesis


    // We must look into the need for booleans akin to "wantsToEnter"
    // It probably depends on which Mutual Exclusion Algorithm we want to implement (Dekker, Peterson, Lamport).

    public static void main(String[] args) {

        ArrayList<Job> listOfJobs = new ArrayList<>(); // Uncompleted Jobs waiting to be dispatched to a Slave
        ArrayList<Job> listOfCompletedJobs = new ArrayList<>(); // Jobs that have been completed by a Slave
        ArrayList<Job> listOfJobsGivenToSlave1 = new ArrayList<>(12);
        ArrayList<Job> listOfJobsGivenToSlave2 = new ArrayList<>(12);

        // Hardcode port number if necessary
        args = new String[] { "30121" , "30122" };

        if (args.length != 2) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }

        int clientPortNumber = Integer.parseInt(args[0]);
        int slavePortNumber = Integer.parseInt(args[1]);

        // this will allow the total amount of threads or clients to connect
        final int MAX_CLIENT_THREADS = 4;  // threads will be numbered 0 -> MAX_THREADS, for now it's 4, but if you want, you can change for more.
        final int MAX_SLAVE_THREADS = 2;

        try (ServerSocket serverClientSocket = new ServerSocket(clientPortNumber);
             ServerSocket serverSlaveSocket = new ServerSocket(slavePortNumber)) {

            // an ArrayList to keep track of all threads
            ArrayList<Thread> listOfClientThreads = new ArrayList<Thread>();
            ArrayList<Thread> listOfSlaveThreads = new ArrayList<Thread>();

            // creation of threads to connect itself and Clients.
            for (int i = 0; i < MAX_CLIENT_THREADS; i++) {
                listOfClientThreads.add(new Thread(new MasterClientThreads(serverClientSocket, i,
                                                                 "Client Thread " + i,
                                                                            listOfJobs)));
            }

            // creation of threads to connect itself and Slaves
            for (int i = 0; i < MAX_SLAVE_THREADS; i++) {
                listOfSlaveThreads.add(new Thread(new MasterSlaveThreads(serverSlaveSocket, i, "Slave Thread " + i)));
            }

            /*
                The Problem with the Above (current) Manner of Creating Threads:
                    We are currently limited to the amount according to MAX_THREADS.
                    Meaning, that we can open up to that many Clients, but if we add
                    one more, the Master (server in this case) will not accept its
                    input. We should look into how to create threads dynamically, which
                    may require that Clients do not receive a dedicated thread, but
                    rather they share the same pool of threads.
             */


            // any "start()" calls need to be placed here
            // starting threads
            for (Thread t : listOfClientThreads) {
                t.start();
            }

            for (Thread t : listOfSlaveThreads) {
                t.start();
            }


//            while (!listOfClientThreads.isEmpty()) {
//                if (listOfJobs.getFirst().getJobType().equals("A") && !SlaveAIsFull()) {
//                    // send to SlaveA
//                    listOfJobsGivenToSlave1.add(new Job(listOfJobs.getFirst().getJobID(),
//                                                        listOfJobs.getFirst().getJobType(),
//                                                         listOfJobs.getFirst().getClientNumber()));
//                    listOfJobs.removeFirst();
//                } else if (listOfJobs.getFirst().getJobType().equals("B") && !SlaveBIsFull()) {
//                    // send to SlaveB
//                    listOfJobsGivenToSlave2.add(new Job(listOfJobs.getFirst().getJobID(),
//                                                        listOfJobs.getFirst().getJobType(),
//                                                        listOfJobs.getFirst().getClientNumber()));;
//                    listOfJobs.removeFirst();
//
//                } else if (listOfJobs.getFirst().getJobType().equals("B") && !SlaveAIsFull()) {
//                    // send to SlaveA a Job with type B
//                    listOfJobsGivenToSlave1.add(new Job(listOfJobs.getFirst().getJobID(),
//                                                        listOfJobs.getFirst().getJobType(),
//                                                        listOfJobs.getFirst().getClientNumber()));;
//                    listOfJobs.removeFirst();
//
//                } else if  (listOfJobs.getFirst().getJobType().equals("A") && !SlaveBIsFull()) {
//                    // send to SlaveB a Job with type A
//                    listOfJobsGivenToSlave2.add(new Job(listOfJobs.getFirst().getJobID(),
//                                                        listOfJobs.getFirst().getJobType(),
//                                                        listOfJobs.getFirst().getClientNumber()));;
//                    listOfJobs.removeFirst();
//
//                }
//            }


            // Mutual Exclusion needs to be placed here.


            // any "join()" calls need to be placed here.
            for (Thread t : listOfClientThreads) {
                try {
                    t.join();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            // join
            for (Thread t : listOfSlaveThreads) {
                try {
                    t.join();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            // joining threads so that they will be more or less in parallel


        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + clientPortNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }  // end catch


    }  // end of main

}  // end of class