//import java.io.*;
//import java.net.*;
//import java.util.ArrayList;
//
//public class MasterToClientThreads extends Thread {
//
//    private ServerSocket serverSocket = null;
//    private int threadId;
//    private String threadName;
//    ArrayList<Job> completedJobsList;
//
//    //constructor
//    public MasterToClientThreads(ServerSocket serverSocket, int id, String threadName, ArrayList<Job> completedJobsList) {
//        this.serverSocket = serverSocket;
//        this.threadId = id;
//        this.threadName = threadName;
//        this.completedJobsList = completedJobsList;
//    }
//
//
//
//    @Override
//    public void run() {
//
//        try (Socket masterToClientSocket = serverSocket.accept();
//             PrintWriter masterToClientResponseWriter = new PrintWriter(masterToClientSocket.getOutputStream(), true);
//             BufferedReader masterToClientRequestReader = new BufferedReader(new InputStreamReader(masterToClientSocket.getInputStream()))
//
//        ){
//
//            System.out.println("Connecting to Client " + this.threadId);
//            System.out.println(this.threadName + " connected on port " + masterToClientSocket.getLocalPort());
//            masterToClientResponseWriter.println("Successfully Connected to Master");
//
//            while(masterToClientSocket != null) {
//
//                while (!completedJobsList.isEmpty()) {
//                    System.out.println("Entered MasterToClient for loop");
//                    synchronized (completedJobsList) {
//                        if (completedJobsList.getFirst().getClientNumber() == this.threadId) {
//
//                        }
////                        for (int i = 0; i < completedJobsList.size(); i++) {
////                            if (completedJobsList.get(i).getClientNumber() == this.threadId) {
////                                masterToClientResponseWriter.println(true);
////                                System.out.println("Informing Client " + threadId
////                                        + " that " + completedJobsList.get(i).getJobID()
////                                        + " has completed.");
////                                masterToClientResponseWriter.println(completedJobsList.get(i).getJobID()
////                                        + " has completed processing.");
////                                completedJobsList.remove(i);
////                            }
////                            masterToClientResponseWriter.println(false);
////                        }
//
//                    }
//
//
//                }
//
//            }
//
//
//        }catch (IOException e) {
//
//            System.out.println(
//                    "Exception caught when trying to listen on port "
//                            + serverSocket.getLocalPort()
//                            + " or listening for a connection"
//            );
//
//            System.out.println(e.getMessage());
//
//        }  // end catch
//
//    }
//}
