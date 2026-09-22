package common;

import java.io.Serializable;

public class Product implements Serializable {
    private String name;
    private double price;
    private int stock;
    private int initialStock; // Αρχικό απόθεμα
    private ProductCategory category;
    private boolean visibleToClient = true;

    public Product(Product other) {
        this.name = other.name;
        this.price = other.price;
        this.stock = other.stock;
        this.initialStock = other.initialStock;
        this.category = other.category;
        this.visibleToClient = other.visibleToClient;
    }

    public Product(String name, double price, int stock, ProductCategory category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.initialStock = stock;
        this.category = category;
    }

    public void setInitialStock(int initialStock) { this.initialStock = initialStock; }
    public int getInitialStock() { return initialStock; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public boolean isVisibleToClient() { return visibleToClient; }

    public void setVisibleToClient(boolean visible) { this.visibleToClient = visible; }

    public boolean buy(int quantity) {
        if (stock >= quantity) {
            stock -= quantity;
            return true;
        }
        return false;
    }

    public void updateStock(int newStock) {
        this.stock = newStock;
    }

    @Override
    public String toString() {
        return name + " - " + price + "€ [" + stock + "] " + (visibleToClient ? "" : "(HIDDEN)");
    }
}