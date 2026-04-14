package a88.jbay;

import a88.jbay.controller.server.ClientHandler;
import com.almasb.fxgl.net.Server;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;

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

        try (ExecutorService executor = Executors.newFixedThreadPool(20);) {
            //for server interface
            executor.submit(() -> {
                System.out.println("Server CLI starting...");

                Scanner sc = new Scanner(System.in);
                String inp; int opt;

                while (true) {
                    inp = sc.nextLine();
                    String[] inps = inp.split(" ");

                    switch (inps[0]) {
                        case "CLI_TEST":
                            System.out.println("Running");
                            break;
                        case "REG_ADMIN":
                            System.out.println("register admin...");
                            //command: REG_ADMIN username password
                            //expect: REG_ADMIN_SUCCESS / REG_ADMIN_FAIL
                            break;
                        case "CLOSE":
                            //command: CLOSE auctionid
                            //expect: CLOSE_SUCCESS / CLOSE_FAIL
                            break;
                        case "DEL":
                            //command: DEL userid
                            //expect: DEL_SUCCESS / DEL_FAIL
                            break;
                        case "UQ":
                            //command: UQ userid [id] / UQ username [username]
                            //expect: UQ_SUCCESS [massive data from server]
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
            executor.submit(() -> {
                System.out.println("Client handler starting...");

                ServerSocket server = new ServerSocket(1234);
                Socket client = null;
                while (true) {
                    client = server.accept();
                    System.out.println("Client connected...");

                    executor.submit(new ClientHandler(client));
                }
            });

            executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n\nSERVER TERMINATED");
        }
    }
}
