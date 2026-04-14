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
