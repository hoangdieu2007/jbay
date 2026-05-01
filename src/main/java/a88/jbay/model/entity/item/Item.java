package a88.jbay.model.entity.item;

import java.io.Serializable;

public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int id;
    protected String name;
    protected String type;
    protected String description;
    protected double initPrice;
    protected byte[] image;

    public Item(String name, String type, String description, double initPrice, byte[] image) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.initPrice = initPrice;
        this.image = image;
    }

    public Item(int id, String name, String type, String description, double initPrice) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.initPrice = initPrice;
    }

    public Item(int id, String name, String type, String description, double initPrice, byte[] image) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.initPrice = initPrice;
        this.image = image;
    }

    public int getId() {return id;}
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
    public byte[] getImage() {
        return image;
    }

    public String toString() {
        return name + " - " + type + " - " + description + " - " + initPrice;
    }
}
