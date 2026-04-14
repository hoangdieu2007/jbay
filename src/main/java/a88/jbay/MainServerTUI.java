package a88.jbay;

import java.util.Scanner;

public class MainServerTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_SERVER_TUI-----------------");
        System.out.println("--------------software infrastructure-------------\n\n");

        Scanner sc = new Scanner(System.in);
        String inp; int opt;

        while (true) {
            inp = sc.nextLine();

            switch (inp) {
                case "REG_ADMIN":
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
    }
}
