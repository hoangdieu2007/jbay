package a88.jbay;

import a88.jbay.model.entity.item.Item;

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
                System.out.println("Choose operation: ");
                System.out.println("1. Create Auction");
                System.out.println("2. List Auctions");
                System.out.println("3. Manage Auction");
                opt = sc.nextInt();
                if (opt == 1) {
                    System.out.println("- Item Info -");
                    System.out.print("Type: "); String type = sc.next();
                    System.out.print("Name: "); String name = sc.next();
                    System.out.print("Description: "); String description = sc.next();
                    System.out.print("Init Price: "); double price = sc.nextDouble();
                    Item item = Item.createItem(type, name, description, price);
                }

                break;

            case 4:
                break;

            default:
                System.out.println("Invalid input");
                break;
        }
    }
}
