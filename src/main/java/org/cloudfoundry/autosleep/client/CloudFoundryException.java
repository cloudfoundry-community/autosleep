package org.cloudfoundry.autosleep.client;

import lombok.Getter;


public class CloudFoundryException extends Exception {
    public enum Type {
        Configuration, InternalError, CallError
    }

    @Getter
    private Type type;

    @Getter
    private int statusCode;


    public CloudFoundryException(Type type, Throwable cause) {
        super(type.name() + ":" + cause.getMessage(), cause);
        this.type = type;
    }

    public CloudFoundryException(Type type, int statusCode, Throwable cause) {
        this(type, cause);
        this.statusCode = statusCode;
    }
}
