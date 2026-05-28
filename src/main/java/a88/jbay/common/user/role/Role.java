package a88.jbay.common.user.role;

public enum Role {
    BAN,
    USER,
    ADMIN;

    public static Role fromString(String roleStr) {
        if (roleStr == null) return BAN;
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BAN;
        }
    }
}