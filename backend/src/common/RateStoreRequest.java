package common;

import java.io.Serializable;

public class RateStoreRequest implements Serializable {
    private String storeName;
    private double rating;
    private final String clientId;

    public RateStoreRequest(String storeName, double rating, String clientId) {
        this.storeName = storeName;
        this.rating = rating;
        this.clientId = clientId;
    }

    public String getStoreName() {
        return storeName;
    }

    public double getRating() {
        return rating;
    }

    public String getClientId() {
        return clientId;
    }
}
