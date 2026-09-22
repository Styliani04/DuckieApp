package common;

import java.io.Serializable;

public class UpdateStockRequest implements Serializable {
    private String storeName;
    private String productName;
    private int newStock;
    private final String clientId;


    public UpdateStockRequest(String storeName, String productName, int newStock, String clientId) {
        this.storeName = storeName;
        this.productName = productName;
        this.newStock = newStock;
        this.clientId = clientId;
    }

    public String getStoreName() { return storeName; }
    public String getProductName() { return productName; }
    public int getNewStock() { return newStock; }

    public String getClientId() {
        return clientId;
    }
}