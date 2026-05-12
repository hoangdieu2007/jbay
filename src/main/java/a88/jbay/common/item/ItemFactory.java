package a88.jbay.common.item;

import java.util.Map;

public interface ItemFactory {
    Item createFromInput(Map<String, String> userInput);
}
