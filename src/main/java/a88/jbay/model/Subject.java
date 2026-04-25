package a88.jbay.model;

import a88.jbay.model.entity.user.User;

public interface Subject {
    void registerObserver(Observer user);
    void removeObserver(Observer user);
    void notifyObservers();
}
