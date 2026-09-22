package common;

import java.io.Serializable;
import java.util.List;

public class SearchRequest implements Serializable {
    private PriceCategory priceCategory; // $, $$, $$$
    private List<FoodCategory> foodCategories;
    private double minStars;
    private double userLat;
    private double userLon;
    private double radiusKm;
    private final String clientId;
    private final long timeoutMs = 10000; // 10-second timeout

    public long getTimeoutMs() {
        return timeoutMs;
    }



    public SearchRequest(PriceCategory priceCategory, List<FoodCategory> foodCategories, double minStars, double userLat, double userLon, double radiusKm, String clientId) {
        this.priceCategory = priceCategory;
        this.foodCategories = foodCategories;
        this.minStars = minStars;
        this.userLat = userLat;
        this.userLon = userLon;
        this.radiusKm = radiusKm;
        this.clientId = clientId;
    }

    public PriceCategory getPriceCategory() {
        return priceCategory;
    }

    public void setPriceCategory(PriceCategory priceCategory) {
        this.priceCategory = priceCategory;
    }

    public List<FoodCategory> getFoodCategories() {
        return foodCategories;
    }

    public void setFoodCategories(List<FoodCategory> foodCategories) {
        this.foodCategories = foodCategories;
    }

    public double getMinStars() {
        return minStars;
    }

    public void setMinStars(int minStars) {
        this.minStars = minStars;
    }

    public double getUserLat() {
        return userLat;
    }

    public void setUserLat(double userLat) {
        this.userLat = userLat;
    }

    public double getUserLon() {
        return userLon;
    }

    public void setUserLon(double userLon) {
        this.userLon = userLon;
    }

    public double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(double radiusKm) {
        this.radiusKm = radiusKm;
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
                "priceCategory='" + priceCategory +
                ", foodCategories=" + foodCategories +
                ", minStars=" + minStars +
                ", userLat=" + userLat +
                ", userLon=" + userLon +
                ", radiusKm=" + radiusKm +
                '}';
    }

    public String getSearchTerm() {
        return clientId + ":" +
                priceCategory + ":" +
                minStars + ":" +
                userLat + "," + userLon + ":" +
                radiusKm;
    }

    public String getClientId() {
        return clientId;
    }
}