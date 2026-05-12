package a88.jbay.di;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple dependency injection container for managing application dependencies.
 * Replaces singleton pattern with proper dependency management.
 */
public class DependencyInjectionContainer {
    private static DependencyInjectionContainer instance;
    private final Map<Class<?>, Object> singletonInstances = new HashMap<>();

    private DependencyInjectionContainer() {}

    public static synchronized DependencyInjectionContainer getInstance() {
        if (instance == null) {
            instance = new DependencyInjectionContainer();
        }
        return instance;
    }

    /**
     * register a singleton instance for a given type.
     */
    public <T> void registerSingleton(Class<T> type, T instance) {
        singletonInstances.put(type, instance);
    }

    /**
     * get an instance of the specified type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> type) {
        Object singleton = singletonInstances.get(type);
        if (singleton != null) {
            return (T) singleton;
        }

        throw new IllegalArgumentException("NO INSTANCE OF: " + type.getName());
    }

    /**
     * clear all registered dependencies (TEST ONLY - DO NOT CALL ON PRODUCTION APP)
     */
    public void clear() {
        singletonInstances.clear();
    }
}
