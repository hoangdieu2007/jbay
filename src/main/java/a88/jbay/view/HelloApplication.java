package a88.jbay.view;

import a88.jbay.di.ApplicationContext;
import a88.jbay.server.DatabaseController;
import a88.jbay.util.JBayLogger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class HelloApplication {
    private static JBayLogger logger;

    public static void main(String[] args) {
        logger = JBayLogger.getLogger(HelloApplication.class);
        logger.info("JBAY Server TUI starting");
        logger.info("------------------JBAY_SERVER_TUI-----------------");
        logger.info("--------------software infrastructure-------------");

        // Initialize dependency injection container
        ApplicationContext.initialize();

        Scanner sc = new Scanner(System.in);

        logger.info("Connect to database:");
        while (true) {
            try {
                logger.info("Enter URL:");
                String url = sc.nextLine();
                logger.info("Enter username:");
                String username = sc.nextLine();
                logger.info("Enter password:");
                String password = sc.nextLine();
                DatabaseController dbController = ApplicationContext.getInstance().getDependency(DatabaseController.class);
                dbController.initializePool(url, username, password);
                dbController.getConnection();
                break;
            } catch (SQLException e) {
                logger.error("Database connection failed, please try again.");
            }
        }
    }
}
