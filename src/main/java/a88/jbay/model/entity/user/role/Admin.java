package a88.jbay.model.entity.user.role;

public class Admin implements State {
    public String bid() {
        return "NOT_PERMITTED";
    }

    public String sell() {
        return "NOT_PERMITTED";
    }

    public String ban() {
        return "BAN";
    }
}
