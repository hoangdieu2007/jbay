package a88.jbay;

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

        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            while (true) {
                System.out.println("Command: login, register, bid, create, end");
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
                                .put("password", password);
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
                System.out.println(response.getMessage());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}