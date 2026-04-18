package a88.jbay.model.entity.item;

import java.util.Map;

public class GenericItemFactory implements ItemFactory{
    @Override
    public Item createFromInput(Map<String, String> userInput) {
        String name = userInput.get("Name");
        String price = userInput.get("Price");
        String desc = userInput.get("Description");

        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Item's name cannot be blanked");
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(price);
        } catch (NumberFormatException | NullPointerException e){
            throw new IllegalArgumentException("Price must be a number");
        }

        GenericItem.GenericBuilder builder = new GenericItem.GenericBuilder().setName(name).setInitPrice(startingPrice);

        if (desc != null && desc.trim().isEmpty()){
            builder.setDescription(desc);
        }

        return builder.build();
    }
}
