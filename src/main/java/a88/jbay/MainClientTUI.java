package a88.jbay;

import a88.jbay.controller.server.AuctionDAO;
import a88.jbay.controller.server.UserDAO;
import a88.jbay.model.entity.user.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class MainClientTUI {
    public static void main(String[] args) {
        System.out.println("ONLY FOR TESTING!!!");
        System.out.println("------------------JBAY_CLIENT_TUI-----------------");
        System.out.println("--------------software infrastructure-------------\n\n");

        //initialize
        User user = new User();

        Scanner sc = new Scanner(System.in);
        String inp; int opt;

        System.out.println("Enter host:");
        String host = sc.nextLine();
        System.out.println("Enter port:");
        int port = sc.nextInt();

        try (
                Socket socket = new Socket(host, port);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
                ) {
            while(true){
                inp = sc.nextLine();

                System.out.println("Sending request to server: " + inp);
                out.println(inp);

                System.out.println("Waiting for server response...");
                String response = in.readLine();

                System.out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n\nPROGRAM FINISHED");
        }
    }
}
