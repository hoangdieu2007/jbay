package a88.jbay.common.item;

public class Electronic extends Item {
    private String brand, model;

    protected Electronic(ElectronicBuilder builder) {
        super(0, builder.name, "ELECTRONIC",builder.description, builder.initPrice);
        this.brand = builder.brand;
        this.model = builder.model;

    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }


    public static class ElectronicBuilder { // static để có thể gọi mà ko cần them chiếu -> cấp phát bộ nhớ lun d
        private String name, brand, model, description;
        private double initPrice;

        public ElectronicBuilder setBrand(String brand) { // để gọi dc liên hoàn
            this.brand = brand;
            return this;  // trả ve tham chiếu để gọi hàm tiep thao --> co the goi theo chuoi
        }

        public ElectronicBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public ElectronicBuilder setInitPrice(double initPrice) {
            this.initPrice = initPrice;
            return this;
        }

        public ElectronicBuilder setModel(String model) {
            this.model = model;
            return this;
        }

        public ElectronicBuilder setDescription(String description) {
            this.description = description;
            return this;
        }
        public Electronic builder(){
            return new Electronic(this);
        }
    }


}
