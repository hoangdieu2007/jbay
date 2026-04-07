package a88.jbay.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// server app code, for user sql data management
public class UserDAO {
    // check the username and password, right in the server
    public boolean checkLogin(String username, String password) {
        DatabaseController databaseController = new DatabaseController();
        Connection connection = databaseController.getConnection();

        String query = "SELECT * FROM users WHERE username = '" + username +"' AND password = '" + password + "'";

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            if  (resultSet.next()) {
                return true;
            } else return false;
        } catch (SQLException e) {
            //remember to change to logging
            e.printStackTrace();
            return false;
        }
    }

    public boolean registerUser(String username, String password) {
        DatabaseController databaseController = new DatabaseController();
        Connection connection = databaseController.getConnection();

        String checkQuery = "SELECT * FROM users WHERE username = '" + username + "'";

        String query = "INSERT INTO users (username, password) VALUES ('" + username + "', '" + password + "')";

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(checkQuery);

            if (resultSet.next()) {
                return false;
            } else {
                statement.execute(query);
                return true;
            }
        } catch (SQLException e) {
            //change to logging please
            e.printStackTrace();
            return false;
        }
    }
}
