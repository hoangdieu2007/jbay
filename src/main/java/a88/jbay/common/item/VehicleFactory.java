package a88.jbay.common.item;

import java.util.Map;

public class VehicleFactory implements ItemFactory{
    @Override
    public Item createFromInput(Map<String, String> userInput) throws FactoryMismatchException {
        String type = userInput.get("Type");
        if (type != null && !type.equalsIgnoreCase("Vehicle")){
            throw new FactoryMismatchException("Wrong factory - This is Vehicle");
        } //catch() xu ly sau
        String name = userInput.get("Name");
        String initPrice = userInput.get("Price");
        String desc = userInput.get("Description");
        String make = userInput.get("Make");
        String year = userInput.get("Year");
        String mileage = userInput.get("Mileage");
        String model = userInput.get("Model");

        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Item's name cannot be blanked");
        }
        double startingPrice;
        try{
            startingPrice = Double.parseDouble(initPrice);
        }catch (NumberFormatException | NullPointerException e){
            throw new IllegalArgumentException("Starting price must be a number");
        }
        Vehicle.VehicleBuilder builder = new Vehicle.VehicleBuilder().setName(name).setInitPrice(startingPrice);
        if (year != null && !year.trim().isEmpty()) {
            try {
                builder.setYear(Integer.parseInt(year));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Year must be a number");
            }
        }
        if (desc != null && !desc.trim().isEmpty()){
            builder.setDescription(desc);
        }
        if (make != null && !make.trim().isEmpty()){ // trim() --> tranh truong hop nhap "   "; check null --> ko sap ct khi trim()
            builder.setMake(make);
        }
        if (mileage!= null && !mileage.trim().isEmpty()){
            builder.setMileage(mileage);
        }
        if (model!= null && !model.trim().isEmpty()){
            builder.setModel(model);
        }
        return builder.build();
    }
}
