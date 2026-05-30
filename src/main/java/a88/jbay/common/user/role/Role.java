package a88.jbay.common.user.role;

public enum Role {
    GUEST,
    BAN,
    USER,
    ADMIN;

    public static Role fromString(String roleStr) {
        if (roleStr == null) return GUEST;
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GUEST;
        }
    }
}