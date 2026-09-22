package common;

import java.io.Serializable;

public class HideProductRequest implements Serializable {
    private String storeName;
    private String productName;
    private final String clientId;

    public HideProductRequest(String storeName, String productName, String clientId) {
        this.storeName = storeName;
        this.productName = productName;
        this.clientId = clientId;
    }

    public String getStoreName() { return storeName; }
    public String getProductName() { return productName; }

    public String getClientId() {
        return clientId;
    }
}
