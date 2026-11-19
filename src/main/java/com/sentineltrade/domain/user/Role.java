package com.sentineltrade.domain.user;

/** <<role-based access>> roles. */
public enum Role {
    TRADER,
    AUDITOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
