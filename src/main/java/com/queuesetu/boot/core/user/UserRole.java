package com.queuesetu.boot.core.user;

/**
 * Canonical role definitions shared across all services, the BFF, and the
 * frontend (via JWT abbreviations).
 *
 * JWT encoding:  roles claim = [[abbrev, scopeId], ...]
 *
 * | Role            | Abbrev | Scope stored in scopeId |
 * |-----------------|--------|--------------------------|
 * | SUPER_ADMIN     | SA     | "" (global)              |
 * | TENANT_ADMIN    | TA     | tenantId                 |
 * | BRANCH_ADMIN    | BA     | branchId                 |
 * | SERVICE_MANAGER | SM     | serviceId                |
 * | STAFF           | ST     | queueId                  |
 * | CUSTOMER        | CU     | "" (global)              |
 */
public enum UserRole {

    SUPER_ADMIN("SA"),
    TENANT_ADMIN("TA"),
    BRANCH_ADMIN("BA"),
    SERVICE_MANAGER("SM"),
    STAFF("ST"),
    CUSTOMER("CU");

    private final String abbrev;

    UserRole(String abbrev) {
        this.abbrev = abbrev;
    }

    /** Short abbreviation embedded in JWT `roles` claim. */
    public String getAbbrev() {
        return abbrev;
    }

    /** Resolve from JWT abbreviation; returns null for unknown values. */
    public static UserRole fromAbbrev(String abbrev) {
        if (abbrev == null) return null;
        for (UserRole r : values()) {
            if (r.abbrev.equalsIgnoreCase(abbrev)) return r;
        }
        return null;
    }
}
