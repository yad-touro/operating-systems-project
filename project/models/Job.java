package com.ydusowitz.assignments.project.models;

public class Job {
    private final int id;
    private final char type;
    private final ClientInfo owner;
    private long cost;

    public Job(int id, char type, ClientInfo owner) {
        this.id = id;
        this.type = type;
        this.owner = owner;
    }

    public int id() {
        return id;
    }

    public char type() {
        return type;
    }

    public ClientInfo owner() {
        return owner;
    }

    public long cost() {
        return cost;
    }

    public void setCost(long c) {
        this.cost = c;
    }
}
