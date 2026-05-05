package com.synapse.taskmanager.model;

public enum UserRole {
    ADMIN("Administrator"),
    MANAGER("Project Manager"),
    USER("Regular User");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
