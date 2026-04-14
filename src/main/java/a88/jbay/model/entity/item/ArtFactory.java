package a88.jbay.model.entity.item;

import java.util.Map;

public class ArtFactory implements ItemFactory{
    @Override
    public Item creatFromInput(Map<String, String> userInput) {
        String name = userInput.get("Name");
        String price = userInput.get("Price");
        String desc = userInput.get("Description");
        String artist = userInput.get("Artist");
        String createYear = userInput.get("Year");
        String medium = userInput.get("Medium");
        if(name == null || name.trim().isEmpty()){
            throw new IllegalArgumentException("Item's name cannot be blanked");
        }
        double startingPrice;
        try {
            startingPrice = Double.parseDouble(price);
        } catch (NumberFormatException | NullPointerException e){
            throw new IllegalArgumentException("Price must be a number");
        }
        Art.ArtBuilder builder = new Art.ArtBuilder().setName(name).setInitPrice(startingPrice);
        int year;
        try {
            year = Integer.parseInt(createYear);
            builder.setCreationYear(year);
        } catch (NumberFormatException | NullPointerException e){
            throw  new IllegalArgumentException("Creation Year must be a number");
        }
        if(desc != null && desc.trim().isEmpty()){
            builder.setDescription(desc);
        }
        if (artist != null && artist.trim().isEmpty()){
            builder.setArtist(artist);
        }
        if (medium != null && medium.trim().isEmpty()){
            builder.setMedium(medium);
        }
        return builder.build();

    }
}
