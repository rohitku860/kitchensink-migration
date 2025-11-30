package com.kitchensink.model;

/**
 * Enum representing user role types in the application.
 * This enum should be used instead of hardcoded role strings throughout the application.
 */
public enum UserRoleType {
    ADMIN("ADMIN", "Administrator role with full access"),
    USER("USER", "Regular user role");
    
    private final String name;
    private final String description;
    
    UserRoleType(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get UserRoleType from string name (case-insensitive)
     * @param name Role name
     * @return UserRoleType enum value
     * @throws IllegalArgumentException if name doesn't match any enum value
     */
    public static UserRoleType fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Role name cannot be null");
        }
        for (UserRoleType roleType : values()) {
            if (roleType.name.equalsIgnoreCase(name)) {
                return roleType;
            }
        }
        throw new IllegalArgumentException("Unknown role name: " + name);
    }
    
    /**
     * Check if the given string matches this role type (case-insensitive)
     */
    public boolean matches(String roleName) {
        return this.name.equalsIgnoreCase(roleName);
    }
}

