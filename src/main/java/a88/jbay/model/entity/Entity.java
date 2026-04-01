package a88.jbay.model.entity;

import java.time.LocalDateTime;

public abstract class Entity {
    protected String id;
    protected final LocalDateTime created;
    protected LocalDateTime modified;
    protected boolean deleted;

    public Entity() {
        this.created = LocalDateTime.now();
        this.modified = this.created;
        this.deleted = false;
    }

    public String getId() {
        return id;
    }
    public LocalDateTime getCreated() {
        return created;
    }
    public LocalDateTime getModified() {
        return modified;
    }
    public boolean isDeleted() {
        return deleted;
    }
}
