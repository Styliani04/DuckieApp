package common;

import java.io.Serializable;

public class SalesQueryRequest implements Serializable {
    public String getClientId() {
        return clientId;
    }

    public enum Type { BY_STORE_TYPE, BY_PRODUCT_CATEGORY }

    private Type type;
    private String value; // Category
    private final String clientId;

    public SalesQueryRequest(Type type, String value, String clientId) {
        this.type = type;
        this.value = value;
        this.clientId = clientId;
    }

    public Type getType() { return type; }
    public String getValue() { return value; }
}
