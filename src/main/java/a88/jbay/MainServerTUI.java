package a88.jbay;

import a88.jbay.controller.server.ClientHandler;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import a88.jbay.system.AuctionSystem;
import a88.jbay.system.UserSystem;
import com.almasb.fxgl.net.Server;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainServerTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_SERVER_TUI-----------------");
        System.out.println("--------------software infrastructure-------------\n\n");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
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
                            System.out.println();
                            break;
                        case "AQ":
                            //command: AQ auctionid [id]
                            //expect: AQ_SUCCESS [massive data from server]
                            break;
                        default:
                            System.out.println("Invalid input");
                            break;
                    }
                }
            });

            //for client handling
            Thread clientHandler = new Thread(() -> {
                System.out.println("Client handler starting...");

                try {
                    ServerSocket server = new ServerSocket(1234);
                    Socket client = null;
                    while (true) {
                        client = server.accept();
                        System.out.println("Client connected...");

                        executor.submit(new ClientHandler(client));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            serverTUI.start();
            clientHandler.start();
            serverTUI.join();
            clientHandler.join();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n\nSERVER TERMINATED");
        }
    }
}
