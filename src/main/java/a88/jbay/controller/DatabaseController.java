package a88.jbay.controller;

import java.sql.*;

// singleton class for database controlling
// requirement: thread safe
public class DatabaseController {
    private Connection connection;

    public Connection getConnection() {
        String url = "jdbc:mysql://localhost:3306/jbay_db";
        String username = "root";
        String password = "220407";
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException exception) {
            //should be replaced with logging
            exception.printStackTrace();
        }

        return connection;
    }
}
