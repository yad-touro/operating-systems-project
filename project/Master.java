package com.ydusowitz.assignments.project;

import com.ydusowitz.assignments.project.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;

/**
 * Master process: accepts any number of slaves and clients, schedules jobs.
 * <p>
 * Usage:
 * java Master <slavePort> <clientPort>
 */
public final class Master {

    private static final Logger LOG = Logger.getLogger(Master.class.getName());

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

    private final ServerSocket slaveServer;
    private final ServerSocket clientServer;

    private final ConcurrentMap<Socket, SlaveInfo> liveSlaves = new ConcurrentHashMap<>();
    private final ConcurrentMap<Socket, ClientInfo> liveClients = new ConcurrentHashMap<>();

    private final BlockingQueue<Job> schedulerQueue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<Integer, Job> inflight = new ConcurrentHashMap<>();

    public Master(int slavePort, int clientPort) throws IOException {
        slaveServer = new ServerSocket(slavePort);
        clientServer = new ServerSocket(clientPort);
        LOG.info(String.format("Listening for slaves on port %d", slavePort));
        LOG.info(String.format("Listening for clients on port %d", clientPort));
        new Thread(this::acceptSlaves, "acceptSlaves").start();
        new Thread(this::acceptClients, "acceptClients").start();
        new Thread(this::runScheduler, "scheduler").start();
    }

    private void acceptSlaves() {
        while (true) {
            try {
                Socket sock = slaveServer.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String hello = in.readLine();
                if (hello == null || !hello.startsWith("SLAVE|")) {
                    LOG.warning(String.format("Unexpected slave greeting from %s: '%s' - closing", sock, hello));
                    sock.close();
                    continue;
                }
                char kind = hello.charAt(hello.indexOf('|') + 1);
                BlockingQueue<String> toSlave = new LinkedBlockingQueue<>();
                SlaveInfo info = new SlaveInfo(sock, kind, toSlave, new AtomicLong());
                liveSlaves.put(sock, info);
                new Thread(() -> slaveIn(sock, info), "slaveIn-" + kind).start();
                new Thread(() -> slaveOut(info), "slaveOut-" + kind).start();
                LOG.info(String.format("Registered slave %s (%c)", sock, kind));
            } catch (IOException e) {
                LOG.severe("[acceptSlaves] " + e.getMessage());
            }
        }
    }

    private void acceptClients() {
        while (true) {
            try {
                Socket sock = clientServer.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                String hello = in.readLine();
                if (!"CLIENT".equals(hello)) {
                    LOG.warning(String.format("Unexpected client greeting from %s: '%s' - closing", sock, hello));
                    sock.close();
                    continue;
                }
                BlockingQueue<String> toClient = new LinkedBlockingQueue<>();
                ClientInfo info = new ClientInfo(sock, toClient);
                liveClients.put(sock, info);
                new Thread(() -> clientIn(sock, info), "clientIn").start();
                new Thread(() -> clientOut(info), "clientOut").start();
                LOG.info(String.format("Registered client %s", sock));
            } catch (IOException e) {
                LOG.severe("[acceptClients] " + e.getMessage());
            }
        }
    }

    private void runScheduler() {
        while (true) {
            try {
                Job job = schedulerQueue.take();
                // pick slave with lowest (pending + cost)
                SlaveInfo best = liveSlaves.values().stream().min(Comparator.comparingLong(s -> s.pendingMs().get() + ((s.kind() == job.type()) ? 2000 : 10000))).orElseThrow();
                long cost = (best.kind() == job.type() ? 2000 : 10000);
                job.setCost(cost);
                best.pendingMs().addAndGet(cost);
                String cmd = String.format("JOB|%d|%d", job.id(), cost);
                best.toSlave().put(cmd);
                inflight.put(job.id(), job);
                LOG.info(String.format("Scheduled job ID=%d to slave %c (cost=%dms)", job.id(), best.kind(), cost));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warning("Scheduler interrupted, shutting down");
                return;
            }
        }
    }

    private void slaveIn(Socket sock, SlaveInfo info) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("DONE|")) continue;
                int id = Integer.parseInt(line.split("\\|")[1]);
                Job job = inflight.remove(id);
                if (job != null) {
                    info.pendingMs().addAndGet(-job.cost());
                    job.owner().toClient().put("DISTRIBUTED SYSTEM – Job Completion Confirmation|" + id);
                    LOG.info(String.format("Received DONE for job ID=%d, notified client", id));
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.severe("[slaveIn] " + e.getMessage());
        } finally {
            liveSlaves.remove(sock);
            LOG.info(String.format("Slave disconnected: %s", sock));
        }
    }

    private void slaveOut(SlaveInfo info) {
        try (PrintWriter out = new PrintWriter(info.commSock().getOutputStream(), true)) {
            while (true) {
                String cmd = info.toSlave().take();
                out.println(cmd);
                LOG.info("Sent to slave: " + cmd);
            }
        } catch (Exception e) {
            LOG.severe("[slaveOut] " + e.getMessage());
        }
    }

    private void clientIn(Socket sock, ClientInfo info) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.startsWith("JOB|")) continue;
                String[] p = line.split("\\|");
                int id = Integer.parseInt(p[1]);
                char type = p[2].charAt(0);
                Job job = new Job(id, type, info);
                schedulerQueue.put(job);
                LOG.info(String.format("Received JOB ID=%d, type=%c from client", id, type));
            }
        } catch (IOException | InterruptedException e) {
            LOG.severe("[clientIn] " + e.getMessage());
        } finally {
            liveClients.remove(sock);
            LOG.info(String.format("ProtocolSimulationClient disconnected: %s", sock));
        }
    }

    private void clientOut(ClientInfo info) {
        try (PrintWriter out = new PrintWriter(info.commSock().getOutputStream(), true)) {
            while (true) {
                String msg = info.toClient().take();
                out.println(msg);
                LOG.info("Sent to client: " + msg);
            }
        } catch (Exception e) {
            LOG.severe("[clientOut] " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            LOG.severe("Usage: java Master <slavePort> <clientPort>");
            System.exit(1);
        }
        int slavePort, clientPort;
        try {
            slavePort = Integer.parseInt(args[0]);
            clientPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            LOG.severe("Ports must be integers: " + e.getMessage());
            return;
        }
        LOG.info(String.format("Starting Master: slavePort=%d, clientPort=%d", slavePort, clientPort));
        new Master(slavePort, clientPort);
    }
}