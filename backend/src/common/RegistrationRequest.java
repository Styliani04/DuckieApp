package common;

import java.io.Serializable;

public class RegistrationRequest implements Serializable {
    private String workerHost;
    private int workerPort;

    public RegistrationRequest(String workerHost, int workerPort) {
        this.workerHost = workerHost;
        this.workerPort = workerPort;
    }

    public String getWorkerHost() {
        return workerHost;
    }

    public int getWorkerPort() {
        return workerPort;
    }
}
