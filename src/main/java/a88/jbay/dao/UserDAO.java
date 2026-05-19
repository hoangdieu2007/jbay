package a88.jbay.dao;

import a88.jbay.common.user.UserData;

import java.util.List;

public interface UserDAO {

    UserData findByUsername(String username);

    UserData findByUserId(int userId);

    UserData findBySessionId(String sessionId);

    boolean existsByUsername(String username);

    int insertUser(
            String username,
            String hashedPassword,
            String role
    );

    boolean insertSession(
            String sessionId,
            int userId
    );

    boolean deleteSession(String sessionId);

    boolean changeUserRole(
            int userId,
            String role
    );

    List<UserData> getAllNormalUsers();
}