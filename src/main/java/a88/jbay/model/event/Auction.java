package a88.jbay.model.event;

import a88.jbay.model.Subject;
import a88.jbay.model.UniqueID;
import a88.jbay.model.entity.item.Item;
import a88.jbay.model.entity.user.User;
import a88.jbay.model.Observer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

//manage auction data and state, all business logic belong to auction system
public class Auction implements Subject {
    private int id;
    private Item item;
    private int sellerId;
    private int winnerId;
    private double startPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // realtime
    private AuctionState auctionState;
    private List<BidTransaction> bidHistory;
    transient private List<Observer> observers;

    public Auction(int id, Item item, int sellerId, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.sellerId = sellerId;
        this.winnerId = -1;
        this.startPrice = item.getInitPrice();
        this.currentPrice = item.getInitPrice();
        this.startTime = startTime;
        this.endTime = endTime;

        this.auctionState = AuctionState.OPENING;
        this.bidHistory = new CopyOnWriteArrayList<>();//sử dụng CopyOnWrite do danh sách này sẽ cho phép chỉnh sửa và xoá list cùng 1 lúc và không bị crash hệ thống
        this.observers = new CopyOnWriteArrayList<>();
    }

    public int getId() {
        return id;
    }
    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void start() {
        this.auctionState = AuctionState.RUNNING;
        this.notifyObservers();
    }

    public void end() {
        this.auctionState = AuctionState.FINISHED;
        this.notifyObservers();
    }

    public void cancel() {
        this.auctionState = AuctionState.CANCELED;
        this.notifyObservers();
    }

    public AuctionState getAuctionState() {
        return auctionState;
    }

    public void setAuctionState(AuctionState auctionState) {
        this.auctionState = auctionState;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void updatePrice(double newPrice, BidTransaction tx) {
        this.currentPrice = newPrice;
        this.winnerId = tx.getUserID();
        this.bidHistory.add(tx);
        this.notifyObservers();
    }

    //observer pattern
    public void registerObserver(Observer observer) {
        observers.add(observer);
        System.out.println("Observer added successfully");
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
        System.out.println("Observer removed successfully");
    }

    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(this);
        }
    }

    //check and change state
    public boolean tick(LocalDateTime now) {
        if (auctionState == AuctionState.OPENING && now.isAfter(startTime)) {
            start();
            return true;
        }
        if (auctionState == AuctionState.RUNNING && now.isAfter(endTime)) {
            end();
            return true;
        }
        return false;
    }


    /*
    this is going to be dead code, all business logic will be handled by the auction system
     */

//    public synchronized void placeBid(BidTransaction bidTransaction) {
//        if (bidTransaction == null) {
//            System.out.println("ERROR: Please enter a bid!");
//            return;
//        }
//        try {
//            this.auctionState.placeBid(this,bidTransaction);
//        }
//        catch(IllegalStateException e) {
//            System.out.println(e.getMessage());
//        }
//        boolean isuccess = false;
//        synchronized (this) {//Đảm bảo tính đa luồng khi muốn chạy qua đây cần có key của this
//            if (bidTransaction.getAmt() <= currentPrice) {
//                System.out.println("ERROR: Please enter a valid amount!");
//            }
//            else {
//                bidHistory.add(bidTransaction);
//                System.out.println("Bid updated successfully");
//                currentPrice = bidTransaction.getAmt();
//                isuccess = true;
//            }
//        }
//        if (isuccess == true) {//đảm bảo thoát vòng khoá rồi mới thực hiện code này nhằm tránh gây tắc nghẽn thời gian do lệnh notify có thể tốn thời gian
//            this.notifyObservers();
//        }
//    }
//
//    public void getCurrentBestBid() {
//        System.out.println((bidHistory.get(bidHistory.size() - 1)).toString());
//    }

}
