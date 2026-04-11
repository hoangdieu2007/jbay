package a88.jbay.model.entity;

import java.time.LocalDateTime;

public abstract class Entity {
    protected String id;

    public Entity() {
    }

    public String getId() {
        return id;
    }
}
