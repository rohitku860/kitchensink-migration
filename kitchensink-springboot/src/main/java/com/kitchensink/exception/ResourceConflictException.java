package com.kitchensink.exception;

public class ResourceConflictException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    private final String field;
    
    public ResourceConflictException(String message, String field) {
        super(message);
        this.field = field;
    }
    
    public String getField() {
        return field;
    }
}

