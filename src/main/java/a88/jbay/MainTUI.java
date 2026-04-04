package a88.jbay;

import java.util.Scanner;

public class MainTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_TUI-----------------");
        System.out.println("----------software infrastructure----------\n\n");

        Scanner sc = new Scanner(System.in);
        String inp; int opt;

        System.out.println("Choose role :");
        System.out.println("1. Admin");
        System.out.println("2. Bidder");
        System.out.println("3. Seller");
        System.out.println("4. Exit");

        opt = sc.nextInt();
        switch (opt) {
            case 1:

                break;

            case 2:
                break;

            case 3:
                break;

            case 4:
                break;

            default:
                System.out.println("Invalid input");
                break;
        }
    }
}
