package a88.jbay.system.user;

import a88.jbay.common.network.Response;
import a88.jbay.common.user.User;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.UserDAO;
import a88.jbay.repository.UserRepository;
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

    public User banUser(int userId) {
        logger.info("Ban user: " + userId);

        // Lấy thông tin thô từ DB để kiểm tra tồn tại và giữ lại username
        UserData userData = userDAO.findByUserId(userId);
        if (userData == null) return null;

        if (!userDAO.changeUserRole(userId, "BAN")) {
            return null;
        }

        // Gửi gói tin Real-time báo tử live xuống máy NẠN NHÂN
        connectionSystem.sendToUser(userId, new Response(true, "BAN_USER", null));

        // Ép hủy các phiên làm việc live của nạn nhân
        Set<ClientConnection> connections = connectionSystem.getConnections().get(userId);
        if (connections != null) {
            for (ClientConnection conn : connections) {
                userSystem.logout(conn.getUserCache().getSessionId());
            }
        }

        updateSystem.unsubscribeUserFromAllAuctions(userId);
        connectionSystem.unregister(userId);

        // Đúc và trả về đối tượng mang Role mới
        return new User(userId, "BAN", userData.username());
    }

    public User unbanUser(int userId) {
        logger.info("Unban user: " + userId);

        UserData userData = userDAO.findByUserId(userId);
        if (userData == null) return null;

        if (!userDAO.changeUserRole(userId, "USER")) {
            return null;
        }

        // Đúc và trả về đối tượng mang Role mới
        return new User(userId, "USER", userData.username());
    }
}