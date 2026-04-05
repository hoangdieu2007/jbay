package a88.jbay.model.entity.item;

import a88.jbay.model.UniqueID;

public abstract class Item {
    protected String id;
    protected String name;
    protected String description;
    protected double initPrice;

    protected Item(String name, String description, double initPrice) {
        this.id = UniqueID.genIID();
        this.name = name;
        this.description = description;
        this.initPrice = initPrice;
    }

    public static Item createItem(String type, String name, String description, double initPrice) {
        switch (type.toLowerCase()) {
            case "electronic":
                return new Electronic(name, description, initPrice);
            case "art":
                return new Art(name, description, initPrice);
            case "vehicle":
                return new Vehicle(name, description, initPrice);
            default:
                return new GenericItem(name, description, initPrice);
        }
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
