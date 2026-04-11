package a88.jbay.controller.server;

// server app code, meant for auction sql data management
public class AuctionDAO {
    private static AuctionDAO instance;

    public static synchronized AuctionDAO getInstance() {
        if (instance == null) {
            instance = new AuctionDAO();
        }
        return instance;
    }
}
