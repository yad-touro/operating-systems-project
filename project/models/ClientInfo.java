package com.ydusowitz.assignments.project.models;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public record ClientInfo(Socket commSock, BlockingQueue<String> toClient) {
}