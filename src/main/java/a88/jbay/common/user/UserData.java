package a88.jbay.common.user;

public record UserData(
        int id,
        String username,
        String role,
        String password
) {}
