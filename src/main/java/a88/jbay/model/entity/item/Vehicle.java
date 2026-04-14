package a88.jbay.model.entity.item;

public class Vehicle extends Item {
    private String make, model;
    private int year;
    private String mileage;
    public Vehicle(VehicleBuilder builder) {
        super(builder.name, builder.description, builder.initPrice);
        this.make = builder.make;
        this.year = builder.year;
        this.mileage = builder.mileage;
    }


    public static class VehicleBuilder{
        private String name, make, description, model;
        private int year;
        private  double initPrice;
        private String mileage;

        public VehicleBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public VehicleBuilder setInitPrice(double initPrice) {
            this.initPrice = initPrice;
            return this;
        }

        public VehicleBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public VehicleBuilder setModel(String model) {
            this.model = model;
            return this;
        }

        public VehicleBuilder setYear(int year) {
            this.year = year;
            return  this;
        }

        public VehicleBuilder setMileage(String mileage) {
            this.mileage = mileage;
            return  this;
        }

        public VehicleBuilder setMake(String make) {
            this.make = make;
            return this;
        }
        public Vehicle build(){
            return new Vehicle(this);
        }
    }
}
