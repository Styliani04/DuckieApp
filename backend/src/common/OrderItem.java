package common;

import java.io.Serializable;


public class OrderItem implements Serializable{
    private String productName;
    private int quantity;

    public OrderItem(String name, int quantity) {
        this.productName = name;
        this.quantity = quantity;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "productName='" + productName +
                ", quantity=" + quantity +
                '}';
    }
}
