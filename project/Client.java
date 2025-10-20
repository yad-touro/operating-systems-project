package com.ydusowitz.assignments.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

/**
 * ProtocolSimulationClient process: connects to Master, submits jobs, and displays completions.
 * One JVM = one client.
 * <p>
 * Protocol:
 * handshake   : CLIENT
 * send JOB    : JOB|<id>|<type>
 * receive DONE: DISTRIBUTED SYSTEM – Job Completion Confirmation|<id>
 * <p>
 * Threads:
 * clientIn     — reads completions from master, logs to console
 * clientOut    — takes JOB lines from queue, writes to master
 * userInput    — reads user commands from console, enqueues JOBs
 */
public final class Client {

    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    static {
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new SimpleFormatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("[%s][%s] %s%n", record.getLoggerName().substring(record.getLoggerName().lastIndexOf('.') + 1), record.getLevel(), record.getMessage());
            }
        });
        LOG.setUseParentHandlers(false);
        LOG.addHandler(ch);
        LOG.setLevel(Level.INFO);
    }

    private final Socket commSock;
    private final BufferedReader masterIn;
    private final PrintWriter masterOut;
    private final BlockingQueue<String> outbound = new LinkedBlockingQueue<>();

    public Client(String host, int port) throws IOException {
        this.commSock = new Socket(host, port);
        this.masterOut = new PrintWriter(commSock.getOutputStream(), true);
        this.masterIn = new BufferedReader(new InputStreamReader(commSock.getInputStream()));

        LOG.info(String.format("Connected to master at %s:%d", host, port));
        masterOut.println("CLIENT");
        LOG.info("Handshake sent: CLIENT");

        new Thread(this::readLoop, "clientIn").start();
        new Thread(this::writeLoop, "clientOut").start();
        new Thread(this::userLoop, "userInput").start();
    }

    /**
     * Reads completion messages from master and logs them.
     */
    private void readLoop() {
        try {
            String line;
            while ((line = masterIn.readLine()) != null) {
                LOG.info(line);
            }
            LOG.info("Connection to master closed");
        } catch (IOException e) {
            LOG.severe("[clientIn] Connection error: " + e.getMessage());
        }
    }

    /**
     * Sends JOB messages from outbound queue to master.
     */
    private void writeLoop() {
        try {
            while (true) {
                String jobLine = outbound.take();
                masterOut.println(jobLine);
                LOG.info("Sent to master: " + jobLine);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warning("[clientOut] Interrupted, shutting down write loop");
        }
    }

    /**
     * Interacts with the user via console to enqueue new jobs.
     */
    private void userLoop() {
        Scanner scanner = new Scanner(System.in);
        try {
            while (true) {
                System.out.print("Enter job type (A/B): ");
                String type = scanner.nextLine().trim().toUpperCase();
                if (!type.matches("[AB]")) {
                    LOG.warning("Invalid job type '" + type + "' — must be A or B");
                    continue;
                }

                System.out.print("Enter job ID (integer): ");
                String idLine = scanner.nextLine().trim();
                if (!idLine.matches("\\d+")) {
                    LOG.warning("Invalid job ID '" + idLine + "' — must be a positive integer");
                    continue;
                }

                String jobLine = String.format("JOB|%s|%s", idLine, type);
                outbound.put(jobLine);
                LOG.info("Enqueued job: " + jobLine);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warning("[userInput] Interrupted, exiting");
        } finally {
            scanner.close();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            LOG.severe("Usage: java ProtocolSimulationClient <host> <port>");
            System.exit(1);
        }
        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            LOG.severe("Invalid port '" + args[1] + "' — must be an integer");
            return;
        }

        try {
            new Client(host, port);
        } catch (IOException e) {
            LOG.severe("Failed to start client: " + e.getMessage());
            System.exit(1);
        }
    }
}
