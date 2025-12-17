
public class Job {
    String jobID;
    String jobType;
    int clientNumber;

    public Job(String jobID, String jobType, int clientNumber) {

        this.jobID = jobID;
        this.jobType = jobType;
        this.clientNumber = clientNumber;

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
}
