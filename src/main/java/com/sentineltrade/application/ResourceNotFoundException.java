package com.sentineltrade.application;

/** Also used when a resource exists but belongs to someone else, so nothing leaks. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource) {
        super(resource + " not found");
    }
}
