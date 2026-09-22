package common;

import java.io.Serializable;
import java.util.List;

public class AddProductsRequest implements Serializable {
    private String storeName;
    private List<Product> products;
    private final String clientId;

    public AddProductsRequest(String storeName, List<Product> products, String clientId) {
        this.clientId = clientId;
        this.storeName = storeName;
        this.products = products;
    }

    public String getStoreName() { return storeName; }
    public List<Product> getProducts() { return products; }

    public String getClientId() {
        return clientId;
    }
}
