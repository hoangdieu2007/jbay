package a88.jbay.model.entity.item;

import a88.jbay.model.UniqueID;

public class Item {
    protected String id;
    protected String name;
    protected String type;
    protected String description;
    protected double initPrice;

    protected Item(String name, String type, String description, double initPrice) {
        this.id = UniqueID.genIID();
        this.name = name;
        this.type = type;
        this.description = description;
        this.initPrice = initPrice;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getType() {return type;}
    public String getDescription() {
        return description;
    }
    public double getInitPrice() {
        return initPrice;
    }
}
