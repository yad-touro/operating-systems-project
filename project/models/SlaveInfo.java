package com.ydusowitz.assignments.project.models;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public record SlaveInfo(Socket commSock, char kind, BlockingQueue<String> toSlave, AtomicLong pendingMs) {
}

