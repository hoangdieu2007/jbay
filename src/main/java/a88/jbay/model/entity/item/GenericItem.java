package a88.jbay.model.entity.item;

public class GenericItem extends Item {
    public GenericItem(GenericBuilder builder) {
        super(0, builder.name, "GENERIC",builder.description, builder.initPrice);
    }

    public static class GenericBuilder{
        private String name, description;
        private double initPrice;

        public GenericBuilder setDescription(String description) {
            this.description = description;
            return  this;
        }

        public GenericBuilder setName(String name) {
            this.name = name;
            return  this;
        }

        public GenericBuilder setInitPrice(double initPrice) {
            this.initPrice = initPrice;
            return this;
        }
        public GenericItem build(){
            return new GenericItem(this);
        }
    }
}
