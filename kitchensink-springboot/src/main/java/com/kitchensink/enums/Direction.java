package com.kitchensink.enums;

public enum Direction {
    NEXT("next"),
    PREV("prev");
    
    private final String value;
    
    Direction(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static Direction fromString(String value) {
        if (value == null || value.isEmpty()) {
            return NEXT;
        }
        String normalized = value.toLowerCase().trim();
        if ("prev".equals(normalized) || "previous".equals(normalized)) {
            return PREV;
        }
        return NEXT;
    }
}

