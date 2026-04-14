package a88.jbay.model.entity.item;

import java.util.Map;

public interface ItemFactory {
    Item creatFromInput(Map<String, String> userInput);
}
