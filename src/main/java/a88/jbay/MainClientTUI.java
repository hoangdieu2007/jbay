package a88.jbay;

import a88.jbay.client.ClientSession;
import a88.jbay.client.ServerConnection;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.event.Auction;
import a88.jbay.model.network.Request;
import a88.jbay.model.network.RequestType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class MainClientTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_CLIENT_TUI-----------------");
        System.out.println("--------------software infrastructure-------------\n\n");


        Scanner sc = new Scanner(System.in);

        //user type host and port until successful connection
        ServerConnection serverConnection = ServerConnection.getInstance();

        while (true) {
            System.out.println("Enter host:");
            String host = sc.nextLine();

            System.out.println("Enter port:");
            int port = Integer.parseInt(sc.nextLine());

            try {
                serverConnection.connect(host, port);
                break;
            } catch (IOException e) {
                System.out.println("Connection failed, please try again.");
            }
        }

        serverConnection.startListener();

        ClientSession clientSession = ClientSession.getInstance();

        while (true) {
            System.out.println("Command: login, register, bid, subscribe, unsubscribe, sell");
            String command = sc.nextLine();

            Request request = switch (command.toLowerCase()) {
                case "status" -> {
                    System.out.println(ClientSession.getInstance().getUser().toString());
                    System.out.println("---Bidder Auctions---");
                    //List<Auction> bidderAuctions = ClientSession.getInstance().getBidderAuctions();
//                    bidderAuctions.forEach(a -> System.out.println(a.toString()));
//                    System.out.println("---Seller Auctions---");
//                    List<Auction> sellerAuctions = ClientSession.getInstance().getSellerAuctions();
//                    //sellerAuctions.forEach(a -> System.out.println(a.toString()));

                    yield null;
                }
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
                case "logout" -> {
                    yield new Request(RequestType.LOGOUT)
                            .put("sessionId", clientSession.getUser().getSessionId());
                }
                case "bid" -> {
                    System.out.println("Auction ID:");
                    int auctionId = Integer.parseInt(sc.nextLine());
                    System.out.println("Bid amount:");
                    double bidAmount = Double.parseDouble(sc.nextLine());

                    yield new Request(RequestType.BID)
                            .put("sessionId", clientSession.getUser().getSessionId())
                            .put("auctionId", auctionId)
                            .put("amount", bidAmount);
                }
                case "subscribe" -> {
                    System.out.println("Auction ID:");
                    int auctionId = Integer.parseInt(sc.nextLine());

                    yield new Request(RequestType.SUBSCRIBE_AUCTION)
                            .put("sessionId", clientSession.getUser().getSessionId())
                            .put("auctionId", auctionId);
                }
                case "unsubscribe" -> {
                    System.out.println("Auction ID:");
                    int auctionId = Integer.parseInt(sc.nextLine());

                    yield new Request(RequestType.UNSUBSCRIBE_AUCTION)
                            .put("sessionId", clientSession.getUser().getSessionId())
                            .put("auctionId", auctionId);
                }
                case "sell" -> {
                    //add item
                    System.out.println("Item name:");
                    String itemName = sc.nextLine();
                    System.out.println("Item type:");
                    String itemType = sc.nextLine();
                    System.out.println("Item description:");
                    String itemDescription = sc.nextLine();
                    System.out.println("Item price:");
                    double itemPrice = Double.parseDouble(sc.nextLine());

                    //later change this to a builder
                    Item item = new Item(itemName, itemType, itemDescription, itemPrice, null);

                    //then create auction
                    System.out.println("Start time (yyyy-MM-dd HH:mm):");
                    String startTime = sc.nextLine();
                    System.out.println("End time (yyyy-MM-dd HH:mm):");
                    String endTime = sc.nextLine();

                    yield new Request(RequestType.SELL)
                            .put("sessionId", clientSession.getUser().getSessionId())
                            .put("item", item)
                            .put("start", LocalDateTime.parse(startTime))
                            .put("end", LocalDateTime.parse(endTime));
                }
                case "cancel" -> {
                    System.out.println("Auction ID:");
                    int auctionId = Integer.parseInt(sc.nextLine());

                    yield new Request(RequestType.CANCEL)
                            .put("sessionId", clientSession.getUser().getSessionId())
                            .put("auctionId", auctionId);
                }
                case "misc" -> {
                    System.out.println("Command:");
                    String misc = sc.nextLine();

                    switch (misc) {
                        case "ls-auction":
                            yield new Request(RequestType.MISC)
                                    .put("command", "ls-auction");
                        default:
                            yield new Request(RequestType.MISC);
                    }
                }
                default -> null;
            };

            if (request == null) {
                System.out.println("Unknown command");
                continue;
            }

            try {
                serverConnection.send(request);
            } catch (IOException e) {
                System.out.println("Failed to send request");
            }
        }
    }
}
