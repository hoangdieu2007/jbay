package a88.jbay;

import a88.jbay.controller.server.AuctionDAO;
import a88.jbay.controller.server.UserDAO;

import java.util.Scanner;

public class MainClientTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_CLIENT_TUI-----------------");
        System.out.println("--------------software infrastructure-------------\n\n");

        Scanner sc = new Scanner(System.in);
        String inp; int opt;

        UserDAO userDAO = new UserDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        while (true) {
            inp = sc.nextLine();
            String[] inps = inp.split(" ");

            // defined commands, later will become the message form between server and client
            switch (inps[0]) {
                case "REG":
                    //command: REG username password
                    //expect: REG_SUCCESS [userid] / REG_FAIL
                    userDAO.registerUser(inps[1], inps[2]);
                    break;
                case "LOGIN":
                    //command: LOGIN username password
                    //expect: LOGIN_SUCCESS [sessionid] / LOGIN_FAIL
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
                default:
                    System.out.println("Please enter a valid option");
                    break;
            }
        }
    }
}
