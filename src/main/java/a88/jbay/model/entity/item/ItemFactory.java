package a88.jbay.model.entity.item;

import java.util.Map;

public interface ItemFactory {
    Item createFromInput(Map<String, String> userInput);
}
