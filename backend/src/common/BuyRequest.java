package common;

import java.io.Serializable;
import java.util.List;

public class BuyRequest implements Serializable {
    private String storeName;
    private List<OrderItem> products;
    private final String clientId;

    public BuyRequest(String storeName, List<OrderItem> products, String clientId) {
        this.clientId = clientId;
        this.storeName = storeName;
        this.products = products;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public List<OrderItem> getProducts() {
        return products;
    }

    public void setProducts(List<OrderItem> products) {
        this.products = products;
    }

    public String getClientId() {
        return clientId;
    }
}