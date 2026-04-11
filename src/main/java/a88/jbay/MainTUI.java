package a88.jbay;

import a88.jbay.controller.server.AuctionDAO;
import a88.jbay.controller.server.UserDAO;

import java.util.Scanner;

public class MainTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_TUI-----------------");
        System.out.println("----------software infrastructure----------\n\n");

        Scanner sc = new Scanner(System.in);
        String inp; int opt;

        UserDAO userDAO = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        while (true) {
            inp = sc.nextLine();
            String[] inps = inp.split(" ");

            // defined commands, later will become the message form between server and client
            switch (inps[0]) {
                case "REG_ADMIN":
                    //command: REG_ADMIN username password
                    // expect: REG_ADMIN_SUCCESS [userid] / REG_ADMIN_FAIL
                    break;
                case "REG":
                    userDAO.registerUser(inps[1], inps[2]);
                    break;
                case "LOGIN":
                    userDAO.checkLogin(inps[1], inps[2]);
                    break;
                case "LOGOUT":
                    //logout
                    break;
                case "BID":
                    // bidding
                    // command: BID userID auctionID amount
                    // expect: BID_SUCCESS [new bid id] / BID_FAIL
                    break;
                case "SELL":
                    // selling
                    // command: SELL userID [item info] [start time] [end time]
                    //expect: SELL_SUCCESS [new auction id] / SELL_FAIL
                    // success means new auction on server database
                    // item info: name, description, price
                    break;
                case "CLOSE":
                    // only for admin or seller owning the auction
                    // command: CLOSE userID auctionID
                    // expect: CLOSE_SUCCESS / CLOSE_FAIL
                    break;
                case "UQ":
                    //TESTING ONLY
                    //user data query, returns server userid, username, password hash
                    //command: UQ username [username] / UQ id [userid]
                    //expect: UQ_RESPONSE [id] [username] [password hash]
                    break;
                case "AQ":
                    //TESTING ONLY
                    //auction data query, returns server auctionid, item info, starting price, seller username, bidder list, bid list
                    // command: AQ auctionid
                    // expect: AQ_RESPONSE [id] [massive load of data printing out]
                    break;
                default:
                    System.out.println("Please enter a valid option");
                    break;
            }
        }
    }
}
