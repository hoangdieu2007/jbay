package a88.jbay.model;

public interface Subject {
    void subscribe(int userId);
    void unsubscribe(int userId);
    void notifyObservers();
}
