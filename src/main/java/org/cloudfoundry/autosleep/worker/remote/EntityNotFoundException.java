package org.cloudfoundry.autosleep.worker.remote;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends Exception {
    public enum EntityType {application, service}

    private EntityType entityType;

    private String entityId;

    public EntityNotFoundException(EntityType entityType, String entityId) {
        this(entityType, entityId, null);
    }

    public EntityNotFoundException(EntityType entityType, String entityId, Throwable cause) {
        super(entityType.name() + "  - " + entityId, cause);
        this.entityType = entityType;
        this.entityId = entityId;
    }
}
