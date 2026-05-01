package a88.jbay.model.entity.item;

public class Art extends Item {
    private String artist;
    private String medium;
    private int creationYear;

    public Art(ArtBuilder builder) {
        super(0, builder.name, "ART",builder.description, builder.initPrice);
        this.artist = builder.artist;
        this.medium = builder.medium;
        this.creationYear = builder.creationYear;
    }

    public static class ArtBuilder {
        // Các thuộc tính kế thừa từ Item
        private String name;
        private String description;
        private double initPrice;

        // Các thuộc tính riêng của Art
        private String artist;
        private String medium;
        private int creationYear;

        // --- Setter cho thuộc tính của Item ---
        public ArtBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public ArtBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public ArtBuilder setInitPrice(double initPrice) {
            this.initPrice = initPrice;
            return this;
        }

        // --- Setter cho thuộc tính của Art ---
        public ArtBuilder setArtist(String artist) {
            this.artist = artist;
            return this;
        }

        public ArtBuilder setMedium(String medium) {
            this.medium = medium;
            return this;
        }

        public ArtBuilder setCreationYear(int creationYear) {
            this.creationYear = creationYear;
            return this;
        }

        // Hàm chốt đối tượng
        public Art build() {
            return new Art(this);
        }
    }
}