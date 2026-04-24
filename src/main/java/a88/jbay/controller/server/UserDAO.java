package a88.jbay.controller.server;

import a88.jbay.model.StringHash;
import a88.jbay.model.UniqueID;

import java.sql.*;

// server app code, for user sql data management
public class UserDAO {
    // check the username and password, right in the server
    private static UserDAO instance;

    public static synchronized UserDAO getInstance() {
        if (instance == null) {
            instance = new UserDAO();
        }
        return instance;
    }

    public String checkLogin(String username, String password) {
        DatabaseController databaseController = new  DatabaseController();
        Connection connection = databaseController.getConnection();
        password = StringHash.hash(password);

        try {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                String sessionID = UniqueID.genSID(
                        resultSet.getInt("id"),
                        resultSet.getString("username")
                );

                String insertQuery = "INSERT INTO sessionids (id, userid) VALUES (?, ?)";
                PreparedStatement insertStmt = connection.prepareStatement(insertQuery);

                insertStmt.setString(1, sessionID);   // assuming sessionID is String
                insertStmt.setInt(2, resultSet.getInt("id"));

                insertStmt.executeUpdate();

                return "LOGIN_SUCCESS " + sessionID;
            } else {
                return "LOGIN_FAIL";
            }
        } catch (SQLException e) {
            //remember to change to logging
            e.printStackTrace();
            return "LOGIN_FAIL";
        }
    }

    public String logOut(String sessionId) {
        return "LOGOUT_SUCCESS";
    }

    public String registerUser(String username, String password) {
        DatabaseController databaseController = new DatabaseController();
        Connection connection = databaseController.getConnection();
        password = StringHash.hash(password);

        try {
            String checkQuery = "SELECT 1 FROM users WHERE username = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setString(1, username);

            ResultSet resultSet = checkStmt.executeQuery();

            if (resultSet.next()) {
                return "REG_FAIL";
            } else {
                String insertQuery = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = connection.prepareStatement(insertQuery);

                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.setString(3, "user");

                insertStmt.executeUpdate();
                return "REG_SUCCESS";
            }
        } catch (SQLException e) {
            //change to logging please
            e.printStackTrace();
            return "REG_FAIL";
        }
    }
}
