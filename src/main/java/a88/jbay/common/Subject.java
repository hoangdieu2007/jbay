package a88.jbay.common;

public interface Subject {
    void subscribe(int userId);
    void unsubscribe(int userId);
    void notifyObservers();
}
