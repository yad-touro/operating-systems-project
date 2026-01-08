

// a Class that keeps track of a Job and it's data. Very important cause it helps
// with keeping track of which Client requested a specific Job to be done.

public class Job {
    private String jobID;
    private String jobType;
    private int clientNumber;
    private boolean completed;

    public Job(String jobID, String jobType, int clientNumber, boolean completed) {

        this.jobID = jobID;
        this.jobType = jobType;
        this.clientNumber = clientNumber;
        this.completed = false;

    }

    public String getJobID() {
        return this.jobID;
    }

    public String getJobType() {
        return this.jobType;
    }

    public int getClientNumber() {
        return this.clientNumber;
    }

    public boolean getCompleted() {
        return this.completed;
    }

    public void setCompleted(boolean bool) {
        this.completed = bool;
    }
}
