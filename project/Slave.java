package com.ydusowitz.assignments.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

/**
 * Slave process (type A or B).
 * One JVM = one slave. Connects to Master at startup.
 * <p>
 * Protocol:
 * handshake   : SLAVE|<kind>
 * receive JOB : JOB|<id>|<sleepMs>
 * send DONE   : DONE|<id>
 * <p>
 * Threads:
 * slaveIn   — reads JOBs from master, sleeps, enqueues DONE
 * slaveOut  — takes DONE messages from queue, writes to master
 */
public final class Slave {

    private static final Logger LOG = Logger.getLogger(Slave.class.getName());

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

    private final char kind;
    private final Socket commSock;
    private final BufferedReader in;
    private final PrintWriter out;
    private final BlockingQueue<String> outbound = new LinkedBlockingQueue<>();

    public Slave(String host, int port, char kind) throws IOException {
        this.kind = kind;
        this.commSock = new Socket(host, port);
        this.out = new PrintWriter(commSock.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(commSock.getInputStream()));

        LOG.info(String.format("Connected to master at %s:%d", host, port));
        out.println("SLAVE|" + kind);
        LOG.info("Handshake sent: SLAVE|" + kind);

        new Thread(this::readLoop, "slaveIn-" + kind).start();
        new Thread(this::writeLoop, "slaveOut-" + kind).start();
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length != 3 || !"JOB".equals(parts[0])) continue;
                String id = parts[1];
                long sleepMs = Long.parseLong(parts[2]);
                LOG.info(String.format("Slave %c processing JOB|%s|%d", kind, id, sleepMs));
                Thread.sleep(sleepMs);
                outbound.put("DONE|" + id);
                LOG.info("Enqueued DONE|" + id);
            }
            LOG.info("Master connection closed, readLoop exiting");
        } catch (IOException e) {
            LOG.severe("[slaveIn] Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warning("[slaveIn] Interrupted, exiting");
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                String msg = outbound.take();
                out.println(msg);
                LOG.info("Sent to master: " + msg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warning("[slaveOut] Interrupted, exiting");
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            LOG.severe("Usage: java Slave <host> <port> <A|B>");
            System.exit(1);
        }
        String host;
        int port;
        char kind;
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
            kind = args[2].charAt(0);
        } catch (Exception e) {
            LOG.severe("Invalid arguments: " + e.getMessage());
            System.exit(1);
            return;
        }
        try {
            new Slave(host, port, kind);
        } catch (IOException e) {
            LOG.severe("Failed to start slave: " + e.getMessage());
            System.exit(1);
        }
    }
}
