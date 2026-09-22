package common;

import java.io.Serializable;

public class GetAllStoresRequest implements Serializable {
    private final String clientId;

    public GetAllStoresRequest(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}