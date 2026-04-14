package a88.jbay;

import a88.jbay.controller.server.AuctionDAO;
import a88.jbay.controller.server.UserDAO;

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

        Scanner sc = new Scanner(System.in);
        String inp; int opt;

        try (
                Socket socket = new Socket("localhost", 1234);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
                ) {
            while(true){
                inp = sc.nextLine();

                out.println(inp);
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
