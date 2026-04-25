package a88.jbay.model.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable {
    private final RequestType type;
    private final Map<String, Object> data;

    public Request(RequestType type) {
        this.type = type;
        this.data = new HashMap<>();
    }

    public RequestType getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Request put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Object get(String key) {
        return data.get(key);
    }
}