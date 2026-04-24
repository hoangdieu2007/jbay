package a88.jbay.model.entity.user;

import java.io.Serializable;

public class Credentials implements Serializable {
    private String role;
    private String username;
    private String sessionId;

    public Credentials(String role, String username, String sessionId) {
        this.role = role;
        this.username = username;
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getSessionId() {
        return sessionId;
    }
}
