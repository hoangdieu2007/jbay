package a88.jbay.model;

public interface Subject {
    void registerObserver();
    void removeObserver();
    void notifyObservers();
}
