package a88.jbay.model.entity.item;

public abstract class Item {
    protected String id;
    protected String name;
    protected String description;
    protected double initPrice;

    public Item(String id, String name, String description, int initPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.initPrice = initPrice;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public double getInitPrice() {
        return initPrice;
    }
}
