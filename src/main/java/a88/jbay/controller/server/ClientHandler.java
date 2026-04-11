package a88.jbay.controller.server;

import a88.jbay.model.entity.user.User;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    Socket socket;
    UserDAO userDAO = UserDAO.getInstance();
    AuctionDAO auctionDAO = AuctionDAO.getInstance();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    private void processCommand(String command) {
        String[] args = command.split(" ");
        String response = "";

        switch (args[0]) {
            //userdao process
            case "LOGIN":
                response =  userDAO.checkLogin(args[1], args[2]);

                //send to client
                System.out.println(response);

                break;
            case "LOGOUT":
                response = userDAO.logOut(args[1]);

                //send to client
                System.out.println(response);
                break;
            case "REG":
                response = userDAO.registerUser(args[0], args[1]);

                //send to client
                System.out.println(response);
                break;

            //auctiondao process
            case "CLOSE":
                //close auction
                break;
            case "DEL":
                //delete user by id / username
                break;

            default:
                break;
        }
    }

    private void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true) //auto flush
        ) {
            String cmd = in.readLine();
            this.processCommand(cmd);
        } catch (IOException e) {
            //replace with proper logging
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.handleClient(socket);
    }
}
