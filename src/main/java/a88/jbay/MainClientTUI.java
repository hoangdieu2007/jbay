package a88.jbay;

import a88.jbay.model.entity.user.User;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;
import a88.jbay.model.network.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class MainClientTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_CLIENT_TUI-----------------");
        System.out.println("--------------software infrastructure-------------\n\n");


        Scanner sc = new Scanner(System.in);

        System.out.println("Enter host:");
        String host = sc.nextLine();

        System.out.println("Enter port:");
        int port = Integer.parseInt(sc.nextLine());

        //current User
        User user = new User();

        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            while (true) {
                System.out.println("Command: login, register, bid, subscribe, unsubscribe, create, end");
                String command = sc.nextLine();

                Request request = switch (command.toLowerCase()) {
                    case "login" -> {
                        System.out.println("Username:");
                        String username = sc.nextLine();
                        System.out.println("Password:");
                        String password = sc.nextLine();

                        yield new Request(RequestType.LOGIN)
                                .put("username", username)
                                .put("password", password);
                    }
                    case "register" -> {
                        System.out.println("Username:");
                        String username = sc.nextLine();
                        System.out.println("Password:");
                        String password = sc.nextLine();

                        yield new Request(RequestType.REGISTER)
                                .put("username", username)
                                .put("password", password)
                                .put("role", "USER");
                    }
                    case "bid" -> {
                        System.out.println("Auction ID:");
                        int auctionId = Integer.parseInt(sc.nextLine());
                        System.out.println("Bid amount:");
                        double bidAmount = Double.parseDouble(sc.nextLine());

                        yield new Request(RequestType.BID)
                                .put("sessionId", user.getSessionId())
                                .put("auctionId", auctionId)
                                .put("amount", bidAmount);
                    }
                    case "subscribe" -> {
                        System.out.println("Auction ID:");
                        int auctionId = Integer.parseInt(sc.nextLine());

                        yield new Request(RequestType.SUBSCRIBE_AUCTION)
                                .put("sessionId", user.getSessionId())
                                .put("auctionId", auctionId);
                    }
                    case "unsubscribe" -> {
                        System.out.println("Auction ID:");
                        int auctionId = Integer.parseInt(sc.nextLine());

                        yield new Request(RequestType.UNSUBSCRIBE_AUCTION)
                                .put("sessionId", user.getSessionId())
                                .put("auctionId", auctionId);
                    }
                    case "sell" -> {
                        //add item


                        //then sell it

                        yield null;
                    }
                    default -> null;
                };

                if (request == null) {
                    System.out.println("Unknown command");
                    continue;
                }

                out.writeObject(request);
                out.flush();

                Response response = (Response) in.readObject();
                String message = response.getMessage();
                System.out.println(message);

                //process response (if it provides any data)
                switch (message) {
                    case "LOGIN_SUCCESS":
                        user = (User) response.getPayload();
                        break;
                    default:
                        System.out.println("No action needed");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
