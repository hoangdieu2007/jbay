package a88.jbay.controller.server;

import a88.jbay.model.UniqueID;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
        DatabaseController databaseController = new DatabaseController();
        Connection connection = databaseController.getConnection();

        String query = "SELECT * FROM users WHERE username = '" + username +"' AND password = '" + password + "'";

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            if  (resultSet.next()) {
                return "LOGIN_SUCCESS " + UniqueID.genSID(resultSet.getInt("id"),  resultSet.getString("username"));
            } else return "LOGIN_FAIL";
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

        String checkQuery = "SELECT * FROM users WHERE username = '" + username + "'";

        String query = "INSERT INTO users (username, password, role) VALUES ('" + username + "', '" + password + "', 'user')";

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(checkQuery);

            if (resultSet.next()) {
                return "REG_FAIL";
            } else {
                statement.execute(query);
                return "REG_SUCCESS";
            }
        } catch (SQLException e) {
            //change to logging please
            e.printStackTrace();
            return "REG_FAIL";
        }
    }
}
