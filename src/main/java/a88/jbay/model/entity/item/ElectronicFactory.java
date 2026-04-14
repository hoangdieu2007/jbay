package a88.jbay.model.entity.item;

import java.util.Map;

public class ElectronicFactory implements ItemFactory{

    @Override
    public Item creatFromInput(Map<String, String> userInput) {
        String name = userInput.get("Name");
        String initPrice = userInput.get("Price");
        String brand  = userInput.get("Brand");
        String model = userInput.get("Model");
        String description = userInput.get("Description");
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Item's name cannot be blanked"); // EP CHUONG TRINH DUNG NGAY LAP TUC
        }
        double startingPrice;
        try {
            startingPrice = Double.parseDouble(initPrice);
        }catch (NumberFormatException  | NullPointerException e){
            throw new IllegalArgumentException("Starting price must be a number");
        }
        // 2 thuoc tinh co ban nhat: name - price
        Electronic.ElectronicBuilder builder = new Electronic.ElectronicBuilder().setName(name).setInitPrice(startingPrice);
        if (description != null && !description.trim().isEmpty()){
            builder.setDescription(description);
        }
        if (brand != null && !brand.trim().isEmpty()){ // trim() --> tranh truong hop nhap "   "; check null --> ko sap ct khi trim()
            builder.setBrand(brand);
        }
        if (model!= null && !model.trim().isEmpty()){
            builder.setModel(model);
        }
        return builder.builder();
    }
}
