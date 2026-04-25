package a88.jbay.model.entity.user.role;

public enum Role {
    GUEST,
    BIDDER,
    SELLER,
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