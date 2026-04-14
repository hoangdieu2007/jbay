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

    private String processCommand(String command) {
        String[] args = command.split(" ");
        String response = "";

        switch (args[0]) {
            case "CLIENT_TEST":
                return "processing client request...";

            //userdao process
            case "LOGIN":
                //command: LOGIN [username] [password]
                //expect: LOGIN_SUCCESS [sessionid] / LOGIN_FAIL
                response =  userDAO.checkLogin(args[1], args[2]);

                //send to client
                return response;
            case "LOGOUT":
                //command: LOGOUT [sessionid]
                //expect: LOGOUT_SUCCESS / LOGOUT_FAIL
                response = userDAO.logOut(args[1]);

                //send to client
                return response;
            case "REG":
                //command: REG [username] [password]
                //expect: REG_SUCCESS [userid] / REG_FAIL
                response = userDAO.registerUser(args[0], args[1]);

                //send to client
                return response;
            case "DEL":
                //command: DEL [userid]
                //expect: DEL_SUCCESS / DEL_FAIL

                //delete user by id / username
                break;

            //auctiondao process
            case "BID":
                //command: BID [userid] [auctionid] [amt]
                //expect: BID_SUCCESS [bidid] / BID_FAIL

                break;
            case "SELL":
                //command: SELL [item info] [seller] [start_time] [end_time]
                //expect: SELL_SUCCESS [auctionid] / SELL_FAIL

                break;
            case "CLOSE":
                //command: CLOSE [auctionid]
                //expect: CLOSE_SUCCESS / CLOSE_FAIL

                //close auction
                break;

            default:
                System.out.println("Invalid command");
                break;
        }

        return "UNDEFINED";
    }

    private void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true) //auto flush
        ) {
            String cmd = in.readLine();
            System.out.println("Received command: " + cmd);

            System.out.println("Processing...");
            String response = processCommand(cmd);

            System.out.println("Response: " + response);
            out.println(cmd);
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
