package common;

import java.io.Serializable;

public class GetStoreRequest implements Serializable {
    private final String storeName;
    private final boolean forceRefresh;
    private final String clientId;

    public GetStoreRequest(String storeName, boolean forceRefresh, String clientId) {
        this.storeName = storeName;
        this.forceRefresh = forceRefresh;
        this.clientId = clientId;
    }

    public String getStoreName() {
        return storeName;
    }

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    @Override
    public String toString() {
        return "GetStoreRequest{" +
                "storeName='" + storeName + '\'' +
                ", forceRefresh=" + forceRefresh +
                '}';
    }

    public String getClientId() {
        return clientId;
    }
}