package a88.jbay;

import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.model.event.Auction;
import a88.jbay.server.ClientHandler;
import a88.jbay.server.ClientService;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.UserSystem;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServerTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_SERVER_TUI-----------------");
        System.out.println("--------------software infrastructure-------------\n\n");

        ClientService clientService = ClientService.getInstance();

        //init systems
        AuctionSystem auctionSystem = AuctionSystem.getInstance();
        UserSystem userSystem = UserSystem.getInstance();

        try {

            //for server interface
            Thread serverTUI = new Thread(() -> {
                System.out.println("Server CLI starting...");

                Scanner sc = new Scanner(System.in);
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
