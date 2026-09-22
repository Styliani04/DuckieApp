package common;

import java.io.Serializable;

public class RegisterReducerRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reducerHost;
    private int reducerPort;

    public RegisterReducerRequest(String reducerHost, int reducerPort) {
        this.reducerHost = reducerHost;
        this.reducerPort = reducerPort;
    }

    public String getReducerHost() {
        return reducerHost;
    }

    public int getReducerPort() {
        return reducerPort;
    }
}
