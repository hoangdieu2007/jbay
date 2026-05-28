package a88.jbay.controller;

import java.util.HashMap;
import java.util.Map;

public class ControllerProvider {
    private static ControllerProvider instance;
    private final Map<Class<?>, Object> controllers = new HashMap<>();
    
    private ControllerProvider() {}
    
    public static ControllerProvider getInstance() {
        if (instance == null) {
            instance = new ControllerProvider();
        }
        return instance;
    }
    
    public void registerController(Object controller) {
        controllers.put(controller.getClass(), controller);
    }

    // generic getter
    public <T> T getController(Class<T> clazz) {
        return clazz.cast(controllers.get(clazz));
    }

    public void clearControllers() {
        controllers.clear();
    }
}
