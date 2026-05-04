package com.queuesetu.boot.core.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.*;

/**
 * Spring Security principal enriched with all role assignments decoded from
 * the JWT {@code roles} claim.
 *
 * <p>Role pairs format: {@code [[abbrev, scopeId], ...]}  where {@code abbrev}
 * is one of the values defined in {@link UserRole} and {@code scopeId} is the
 * UUID of the resource the role is scoped to (empty string for global roles).
 *
 * <h3>Convenience check methods</h3>
 * <ul>
 *   <li>{@link #isSuperAdmin()} — global admin</li>
 *   <li>{@link #isTenantAdmin(String)} — admin for a specific tenant</li>
 *   <li>{@link #isBranchAdmin(String)} — admin for a specific branch</li>
 *   <li>{@link #isServiceManager(String)} — manager for a specific service</li>
 *   <li>{@link #isStaffForQueue(String)} — staff assigned to a specific queue</li>
 *   <li>{@link #canOperateQueue(String, String, String, String)} — combined check</li>
 * </ul>
 */
public class UserDetail extends User {

    private final UUID userId;
    private final String email;
    /** Each entry is [abbrev, scopeId]. Never null; may be empty. */
    private final List<List<String>> rolesPairs;

    /** Legacy constructor kept for backwards compatibility. */
    public UserDetail(String username, String password,
                      Collection<? extends GrantedAuthority> authorities,
                      UUID userId, UUID branchId, UUID tenantId, String email) {
        super(username, password, authorities);
        this.userId = userId;
        this.email = email;
        this.rolesPairs = Collections.emptyList();
    }

    public UserDetail(String username, String password,
                      Collection<? extends GrantedAuthority> authorities,
                      UUID userId, String email,
                      List<List<String>> rolesPairs) {
        super(username, password, authorities);
        this.userId = userId;
        this.email = email;
        this.rolesPairs = rolesPairs != null ? rolesPairs : Collections.emptyList();
    }

    // ── Basic accessors ───────────────────────────────────────────────────

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    /** Raw role pairs decoded from JWT. Prefer the typed check methods below. */
    public List<List<String>> getRolesPairs() { return rolesPairs; }

    // ── Low-level helpers ─────────────────────────────────────────────────

    /** Returns true if the user has at least one assignment with this abbreviation. */
    public boolean hasRole(String abbrev) {
        if (abbrev == null) return false;
        for (List<String> pair : rolesPairs) {
            if (pair != null && !pair.isEmpty() && abbrev.equalsIgnoreCase(pair.get(0))) return true;
        }
        return false;
    }

    /** All scope IDs the user holds for the given abbreviation. */
    public List<String> getIdsForRole(String abbrev) {
        List<String> ids = new ArrayList<>();
        if (abbrev == null) return ids;
        for (List<String> pair : rolesPairs) {
            if (pair != null && pair.size() >= 2 && abbrev.equalsIgnoreCase(pair.get(0))) {
                String id = pair.get(1);
                if (id != null && !id.isBlank()) ids.add(id);
            }
        }
        return ids;
    }

    public List<UUID> getIdsForRoleAsUUIDs(String abbrev) {
        List<UUID> ids = new ArrayList<>();
        for (String s : getIdsForRole(abbrev)) {
            try { ids.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        return ids;
    }

    // ── Typed role checks (use these everywhere) ──────────────────────────

    /** Global super-admin across the entire platform. */
    public boolean isSuperAdmin() {
        return hasRole(UserRole.SUPER_ADMIN.getAbbrev());
    }

    /** Tenant admin for the given tenantId (or any tenant if tenantId is null). */
    public boolean isTenantAdmin(String tenantId) {
        if (isSuperAdmin()) return true;
        if (tenantId == null || tenantId.isBlank()) return hasRole(UserRole.TENANT_ADMIN.getAbbrev());
        for (List<String> pair : rolesPairs) {
            if (pair != null && pair.size() >= 2
                    && UserRole.TENANT_ADMIN.getAbbrev().equalsIgnoreCase(pair.get(0))
                    && tenantId.equalsIgnoreCase(pair.get(1))) return true;
        }
        return false;
    }

    /** Branch admin for the given branchId (or any branch if branchId is null). */
    public boolean isBranchAdmin(String branchId) {
        if (isSuperAdmin()) return true;
        if (branchId == null || branchId.isBlank()) return hasRole(UserRole.BRANCH_ADMIN.getAbbrev());
        for (List<String> pair : rolesPairs) {
            if (pair != null && pair.size() >= 2
                    && UserRole.BRANCH_ADMIN.getAbbrev().equalsIgnoreCase(pair.get(0))
                    && branchId.equalsIgnoreCase(pair.get(1))) return true;
        }
        return false;
    }

    /** Service manager for the given serviceId (or any service if serviceId is null). */
    public boolean isServiceManager(String serviceId) {
        if (isSuperAdmin()) return true;
        if (serviceId == null || serviceId.isBlank()) return hasRole(UserRole.SERVICE_MANAGER.getAbbrev());
        for (List<String> pair : rolesPairs) {
            if (pair != null && pair.size() >= 2
                    && UserRole.SERVICE_MANAGER.getAbbrev().equalsIgnoreCase(pair.get(0))
                    && serviceId.equalsIgnoreCase(pair.get(1))) return true;
        }
        return false;
    }

    /** Staff assigned to the given queueId (or any queue if queueId is null). */
    public boolean isStaffForQueue(String queueId) {
        if (isSuperAdmin()) return true;
        if (queueId == null || queueId.isBlank()) return hasRole(UserRole.STAFF.getAbbrev());
        for (List<String> pair : rolesPairs) {
            if (pair != null && pair.size() >= 2
                    && UserRole.STAFF.getAbbrev().equalsIgnoreCase(pair.get(0))
                    && queueId.equalsIgnoreCase(pair.get(1))) return true;
        }
        return false;
    }

    /**
     * Returns true if the user can perform staff-level queue operations
     * (call next / mark completed) on a queue that belongs to the given
     * tenant, branch, and service.
     *
     * <p>Hierarchy: SuperAdmin ⊃ TenantAdmin ⊃ BranchAdmin ⊃ ServiceManager ⊃ Staff(queue)
     */
    public boolean canOperateQueue(String queueId, String tenantId, String branchId, String serviceId) {
        return isSuperAdmin()
                || isTenantAdmin(tenantId)
                || isBranchAdmin(branchId)
                || isServiceManager(serviceId)
                || isStaffForQueue(queueId);
    }
}

