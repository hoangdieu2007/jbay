package a88.jbay.model.event;

import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.Seller;
import a88.jbay.model.event.auctionstate.AuctionState;
import a88.jbay.model.event.auctionstate.OpeningState;
import a88.jbay.model.Subject;
import a88.jbay.model.Observer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction {
    private String id;
    private Item item;
    private Seller seller;
    private double startPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    //realtime
    private AuctionState auctionState;
    private List<BidTransaction> bidHistory;
    private List<Observer> observers;

    public Auction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = UniqueID.genAID();
        this.item = item;
        this.seller = seller;
        this.startPrice = item.getInitPrice();
        this.currentPrice = item.getInitPrice();
        this.startTime = startTime;
        this.endTime = endTime;

        this.auctionState = new OpeningState();
        this.bidHistory = new CopyOnWriteArrayList<>();//sử dụng CopyOnWrite do danh sách này sẽ cho phép chỉnh sửa và xoá list cùng 1 lúc và không bị crash hệ thống
        this.observers = new CopyOnWriteArrayList<>();
    }

    public String getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }

    public void start() {this.auctionState.start(this);}
    public void end() {
        this.auctionState.end(this);
    }
    // public void cancel() {this.auctionState.cancel();}

    public AuctionState getAuctionState() {
        return auctionState;
    }

    public void setAuctionState(AuctionState state) {
        this.auctionState = state;
    }

    public void registerObserver(Observer observer) {
        observers.add(observer);
        System.out.println("Observer added successfully");
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
        System.out.println("Observer removed successfully");
    }

    public void notifyObservers() {
//        for (Observer observer : observers) {
//            observer.update(this);
//        }
    }

    public synchronized void placeBid(BidTransaction bidTransaction) {
        if (bidTransaction == null) {
            System.out.println("ERROR: Please enter a bid!");
            return;
        }
        try {
            this.auctionState.placeBid(this,bidTransaction);
        }
        catch(IllegalStateException e) {
            System.out.println(e.getMessage());
        }
        boolean isuccess = false;
        synchronized (this) {//Đảm bảo tính đa luồng khi muốn chạy qua đây cần có key của this
            if (bidTransaction.getAmt() <= currentPrice) {
                System.out.println("ERROR: Please enter a valid amount!");
            }
            else {
                bidHistory.add(bidTransaction);
                System.out.println("Bid updated successfully");
                currentPrice = bidTransaction.getAmt();
                isuccess = true;
            }
        }
        if (isuccess == true) {//đảm bảo thoát vòng khoá rồi mới thực hiện code này nhằm tránh gây tắc nghẽn thời gian do lệnh notify có thể tốn thời gian
            this.notifyObservers();
        }
    }

    public void getCurrentBestBid() {
        System.out.println((bidHistory.get(bidHistory.size() - 1)).toString());
    }
}
