package a88.jbay.model.entity.item;

public abstract class Item {
    protected String id;
    protected String name;
    protected String description;
    protected double initPrice;

    protected Item(String id, String name, String description, double initPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.initPrice = initPrice;
    }

    public static Item createItem(String type, String id, String name, String description, double initPrice) {
        switch (type.toLowerCase()) {
            case "electronic":
                return new Electronic(id, name, description, initPrice);
            case "art":
                return new Art(id, name, description, initPrice);
            case "vehicle":
                return new Vehicle(id, name, description, initPrice);
            default:
                return new GenericItem(id, name, description, initPrice);
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
