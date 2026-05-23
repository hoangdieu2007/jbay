package a88.jbay.dao;

import a88.jbay.common.user.UserData;

import java.util.List;

public interface UserDAO {

    UserData findByUsername(String username);

    UserData findByUserId(int userId);

    boolean existsByUsername(String username);

    int insertUser(
            String username,
            String hashedPassword,
            String role
    );

    boolean changeUserRole(
            int userId,
            String role
    );

    List<UserData> getAllNormalUsers();
}
