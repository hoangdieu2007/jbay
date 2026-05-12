package a88.jbay.view;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.common.auction.Auction;
import a88.jbay.server.ClientConnection;
import a88.jbay.server.ClientService;
import a88.jbay.server.DatabaseController;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.UpdateSystem;
import a88.jbay.system.UserSystem;
import a88.jbay.util.JBayLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Scanner;

public class MainServerTUI {
    private static final Logger log = LoggerFactory.getLogger(MainServerTUI.class);
    private static JBayLogger logger;
    
    public static void main(String[] args) {
        logger = JBayLogger.getLogger(MainServerTUI.class);
        logger.info("JBAY Server TUI starting");
        logger.info("------------------JBAY_SERVER_TUI-----------------");
        logger.info("--------------software infrastructure-------------");

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
                DatabaseController.setCredentials(url, username, password);
                DatabaseController.getInstance().getConnection();
                break;
            } catch (SQLException e) {
                logger.error("Database connection failed, please try again.");
            }
        }

        ClientService clientService = ClientService.getInstance();

        /**
         * init DAOs
         */
        AuctionDAO auctionDAO = AuctionDAO.getInstance();
        UserDAO userDAO = UserDAO.getInstance();
        BidDAO bidDAO = BidDAO.getInstance();
        ItemDAO itemDAO = ItemDAO.getInstance();

        //init systems
        DatabaseController dbController = DatabaseController.getInstance();
        AuctionSystem auctionSystem = AuctionSystem.getInstance();
        UserSystem userSystem = UserSystem.getInstance();
        UpdateSystem updateSystem = UpdateSystem.getInstance();

        try {

            //for server interface
            Thread serverTUI = new Thread(() -> {
                System.out.println("Server CLI starting...");

                String command; int opt;

                while (true) {
                    command = sc.nextLine();

                    switch (command) {
                        case "CLI_TEST":
                            System.out.println("Running");
                            break;
                        case "REG_ADMIN":
                            System.out.println("register admin...");
                            System.out.println("Username:");
                            String username = sc.nextLine();
                            System.out.println("Password:");
                            String password = sc.nextLine();

                            UserSystem.getInstance().register(username, password, "ADMIN");

                            break;
                        case "CANCEL":
                            System.out.println("Auction ID:");
                            int auctionId = sc.nextInt();

                            AuctionSystem.getInstance().cancelAuction(auctionId);

                            break;
                        case "BAN":
                            System.out.println("User ID:");
                            int userId = sc.nextInt();

                            UserSystem.getInstance().banUser(userId);

                            break;
                        case "UQ":
                            //user query
                            System.out.println("User ID:");
                            userId = sc.nextInt();

                            break;
                        case "AQ":
                            //command: AQ auctionid [id]
                            //expect: AQ_SUCCESS [massive data from server]

                            System.out.println("Auction ID:");
                            auctionId = sc.nextInt();

                            if (auctionId == -1) {
                                for (Auction auction : AuctionSystem.getInstance().getActiveAuctionList()) {
                                    System.out.println(auction);
                                }
                            }

                            break;
                        case "NUKE":
                            // nuke the database
                            break;
                        case "RELOAD":
                            auctionSystem.loadActiveAuctions();
                            break;
                        case "UPDATE":
                            //update all users
                            break;
                        case "LS_CONN":
                            // list all connections
                            updateSystem.getConnections().forEach((uid, clientConnection) -> {
                                logger.info("UserID: " + uid);
                                for (ClientConnection client : clientConnection) {
                                    logger.info("Connection ID: " + client.getConnectionId());
                                }
                            });
                            break;
                        default:
                            System.out.println("Invalid input");
                            break;
                    }
                }
            });

            //for client handling
            clientService.setupServerSocket(1234);

            serverTUI.start();
            clientService.startService();

            //service runs as long as TUI is up and running
            serverTUI.join();
            clientService.stopService();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n\nSERVER TERMINATED");
        }
    }
}
