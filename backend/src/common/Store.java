package common;

import java.util.*;
import java.io.Serializable;


public class Store implements Serializable {
    private String name;
    private Location location;
    private String logoPath;
    private double rating;
    private int reviews;
    private FoodCategory foodCategory;
    private List<Product> products = new ArrayList<>();
    private double totalSales = 0.0;
    private PriceCategory priceCategory;
    private Map<String, Integer> salesPerProduct = new HashMap<>();


    public Store() {
        name = null;
        location = new Location(0, 0);
        logoPath = null;
        rating = 0.0;
        reviews = 0;
        foodCategory = null;
    }

    public Store(Store other) {
        System.out.println("[DEBUG] Creating Store copy from: " + other.name);
        this.name = other.name;
        this.location = new Location(other.location.getLat(), other.location.getLon());
        this.logoPath = other.logoPath;
        this.rating = other.rating;
        this.reviews = other.reviews;
        this.foodCategory = other.foodCategory;
        this.totalSales = other.totalSales;
        this.priceCategory = other.priceCategory;

        // Deep copy products
        this.products = new ArrayList<>();
        for (Product p : other.products) {
            System.out.println("[DEBUG] Copying product: " + p.getName() +
                    " (Stock: " + p.getStock() + ")");
            this.products.add(new Product(p));
        }

        // Deep copy sales data
        this.salesPerProduct = new HashMap<>(other.salesPerProduct);
    }

    public void copyFrom(Store other) {
        if (other == null) {
            throw new IllegalArgumentException("Source store cannot be null");
        }

        this.name = other.name;
        this.rating = other.rating;
        this.reviews = other.reviews;
        this.totalSales = other.totalSales;
        this.priceCategory = other.priceCategory;
        this.foodCategory = other.foodCategory;
        this.logoPath = other.logoPath;

        this.location = new Location(other.location.getLat(), other.location.getLon());

        // Synchronize during product copy to ensure thread safety
        synchronized (this) {
            // Clear existing products
            this.products.clear();

            // Create new Product instances for deep copy
            for (Product otherProduct : other.products) {
                Product newProduct = new Product(
                        otherProduct.getName(),
                        otherProduct.getPrice(),
                        otherProduct.getStock(),
                        otherProduct.getCategory()
                );
                newProduct.setVisibleToClient(otherProduct.isVisibleToClient());
                newProduct.setInitialStock(otherProduct.getInitialStock());
                this.products.add(newProduct);
            }

            // Deep copy sales data
            this.salesPerProduct = new HashMap<>();
            other.salesPerProduct.forEach((k, v) ->
                    this.salesPerProduct.put(k, v)
            );
        }
    }

    public void updatePriceCategory() {
        this.priceCategory = getPriceCategory(); // υπολογισμός βάσει τιμών
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviews() {
        return reviews;
    }

    public void setReviews(int reviews) {
        this.reviews = reviews;
    }

    public FoodCategory getFoodCategory() {
        return foodCategory;
    }

    public void setFoodCategory(FoodCategory foodCategory) {
        this.foodCategory = foodCategory;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public void deleteProduct(Product p){this.products.remove(p);}

    public double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }

    public synchronized void addSale(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Sale amount must be positive");
        }
        this.totalSales += amount;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Store{name='").append(name)
                .append("', location=").append(location)
                .append(", logoPath='").append(logoPath)
                .append("', rating=").append(rating)
                .append(", reviews=").append(reviews)
                .append(", foodCategory=").append(foodCategory)
                .append(", products=[");

        // Add products with their stock levels
        for (Product p : products) {
            sb.append(p.getName())
                    .append(" - ")
                    .append(p.getPrice())
                    .append("€ [")
                    .append(p.getStock())
                    .append("], ");
        }

        // Remove trailing comma if products exist
        if (!products.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("], totalSales=").append(totalSales)
                .append(", priceCategory='").append(getPriceCategory())
                .append("'}");

        return sb.toString();
    }


    public PriceCategory getPriceCategory() {
        double avg = products.stream().mapToDouble(p -> p.getPrice()).average().orElse(0);
        if (avg <= 5) return PriceCategory.$;
        if (avg <= 15) return PriceCategory.$$;
        return PriceCategory.$$$;
    }

    //tsekarei an to store tairiazei me ta filtra toy pelath
    public boolean matches(SearchRequest req) {
        boolean hasMatchingCategory = req.getFoodCategories().contains(this.foodCategory);

        double dist = location.distanceTo(new Location(req.getUserLat(), req.getUserLon()));

        return this.getPriceCategory().isCheaperThan(req.getPriceCategory()) &&
                this.getRating() >= req.getMinStars() &&
                hasMatchingCategory &&
                dist <= req.getRadiusKm();
    }

    public synchronized boolean buyProduct(OrderItem item) {
        Product p = findProduct(item.getProductName());
        if (p != null && p.getStock() >= item.getQuantity()) {
            if (p.getInitialStock() == 0) {
                p.setInitialStock(p.getStock() + item.getQuantity()); // Αρχικοποίηση
            }
            p.setStock(p.getStock() - item.getQuantity());
            return true;
        }
        return false;
    }

    // ta proionta poy exoyn ginei delete den emfanizontai ston pelath
    public List<Product> getVisibleProducts() {
        return products.stream()
                .filter(Product::isVisibleToClient)
                .toList();
    }

    public synchronized void updateProductStock(String productName, int newStock) {
        Product product = findProduct(productName);
        if (product != null) {
            product.setStock(newStock);
        }
    }

    public synchronized void addProduct(Product newProduct) {
        // Remove existing product if present
        products.removeIf(p -> p.getName().equalsIgnoreCase(newProduct.getName()));
        products.add(new Product(newProduct)); // Use copy constructor
        updatePriceCategory();
    }

    public synchronized void setProductVisibility(String productName, boolean visible) {
        Product product = findProduct(productName);
        if (product != null) {
            product.setVisibleToClient(visible);
        }
    }

    public Product findProduct(String productName) {
        for (Product p : products) {
            if (p.getName().equalsIgnoreCase(productName)) {
                return p;
            }
        }
        return null;
    }
}
