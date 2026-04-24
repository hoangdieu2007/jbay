package a88.jbay.model;

public interface Observer {
    void update (Object obj, double currentPrice);
    void setChanged();
    void notifyObservers();
}