package a88.jbay.system.user;

import a88.jbay.common.network.Response;
import a88.jbay.dao.UserDAO;
import a88.jbay.server.ClientConnection;
import a88.jbay.system.update.ConnectionSystem;
import a88.jbay.system.update.UpdateSystem;
import a88.jbay.system.user.UserSystem;
import a88.jbay.util.JBayLogger;

import java.util.Set;

public class AdminService {

    private final UserDAO userDAO;
    private final ConnectionSystem connectionSystem;
    private final UpdateSystem updateSystem;
    private final UserSystem userSystem;
    private final JBayLogger logger;

    public AdminService(
            UserDAO userDAO,
            ConnectionSystem connectionSystem,
            UpdateSystem updateSystem,
            UserSystem userSystem
    ) {
        this.userDAO = userDAO;
        this.connectionSystem = connectionSystem;
        this.userSystem = userSystem;
        this.updateSystem = updateSystem;
        this.logger = JBayLogger.getLogger(AdminService.class);
    }

    public boolean banUser(int userId) {
        logger.info("Ban user: " + userId);

        if (userDAO.findByUserId(userId) == null) return false;

        if (!userDAO.changeUserRole(userId, "BAN")) {
            return false;
        }

        // notify client
        connectionSystem.sendToUser(
                userId,
                new Response(true, "BAN_USER", null)
        );

        // force disconnect sessions
        Set<ClientConnection> connections = connectionSystem.getConnections().get(userId);

        if (connections != null) {
            for (ClientConnection conn : connections) {
                userSystem.logout(conn.getUserCache().getSessionId());
            }
        }

        updateSystem.unsubscribeUserFromAllAuctions(userId);
        connectionSystem.unregister(userId);

        return true;
    }

    public boolean unbanUser(int userId) {
        logger.info("Unban user: " + userId);
        return userDAO.changeUserRole(userId, "USER");
    }
}