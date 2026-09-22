package common;

import java.io.Serializable;

public class Location implements Serializable {
    private double lat;
    private double lon;

    public Location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double distanceTo(Location other) {
        double dx = this.lat - other.lat;
        double dy = this.lon - other.lon;
        return Math.sqrt(dx*dx + dy*dy) * 111; // Rough km
    }

    @Override
    public String toString() {
        return "Location{" +
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }

    public double getLat() { return lat; }
    public double getLon() { return lon; }
}