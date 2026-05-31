package a88.jbay.system.user;

import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;
import a88.jbay.common.user.role.Role;
import a88.jbay.di.ApplicationContext;
import a88.jbay.data.UserRepository;
import a88.jbay.util.JBayLogger;
import a88.jbay.util.StringHash;

import java.util.UUID;

public class UserSystem {

    private final UserRepository userRepository;
    private final JBayLogger logger;

    public UserSystem(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.logger = JBayLogger.getLogger(UserSystem.class);
    }

    public static UserSystem getInstance() {
        return ApplicationContext.getInstance()
                .getDependency(UserSystem.class);
    }

    public User login(String username, String password) {

        logger.debug("Login attempt: " + username);

        UserData userData =
                userRepository.findByUsername(username);

        if (userData == null) {
            logger.warn("User not found: " + username);
            return null;
        }

        if (!StringHash.hash(password)
                .equals(userData.password())) {

            logger.warn("Invalid password: " + username);
            return null;
        }

        String sessionId = UUID.randomUUID().toString();

        User user = new User(
                userData.id(),
                Role.fromString(userData.role()),
                username,
                sessionId
        );

        if (!userRepository.createSession(sessionId, user)) {

            logger.error("Failed to create session");
            return null;
        }

        return user;
    }

    public boolean register(String username, String password, String role, byte[] qrCode) {
        logger.debug("Register: " + username);
        return userRepository.createUser(username, StringHash.hash(password), role, qrCode);
    }

    public void logout(String sessionId) {
        logger.debug("Logout: " + sessionId);
        userRepository.deleteSession(sessionId);
    }

    public User findBySessionId(String sessionId) {
        return userRepository.findBySessionId(sessionId);
    }

    public java.util.List<User> getAllNormalUsersForAdmin() {
        return userRepository.getAllNormalUsers();
    }

    public User getUserByName(String username) {
        UserData userData = userRepository.findByUsername(username);
        if (userData != null) {
            return new User(userData.id(), Role.fromString(userData.role()), userData.username());
        }
        return null;
    }

    public byte[] getQr(int userId) {
        return userRepository.getQr(userId);
    }
}