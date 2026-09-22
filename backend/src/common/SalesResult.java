package common;

import java.io.Serializable;
import java.util.Map;

public class SalesResult implements Serializable {
    private Map<String, Integer> storeSales;
    private int total;

    public SalesResult(Map<String, Integer> storeSales, int total) {
        this.storeSales = storeSales;
        this.total = total;
    }

    public Map<String, Integer> getStoreSales() { return storeSales; }
    public int getTotal() { return total; }
}