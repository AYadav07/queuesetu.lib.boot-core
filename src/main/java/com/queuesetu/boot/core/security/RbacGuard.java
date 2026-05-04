package com.queuesetu.boot.core.security;

import com.queuesetu.boot.core.user.UserDetail;
import org.springframework.security.core.Authentication;

/**
 * Spring Security expression bean exposed as {@code @rbac} in SpEL.
 *
 * <p>Designed for use with {@code @PreAuthorize} in BFF controllers:
 * <pre>{@code
 *   @PreAuthorize("@rbac.isTenantAdmin(authentication, #tenantId)")
 *   @PreAuthorize("@rbac.isBranchManageable(authentication, #branchId, #body.tenantId)")
 *   @PreAuthorize("@rbac.canOperateQueue(authentication, #queueId)")
 * }</pre>
 *
 * <p><strong>Design principle — two-tier enforcement:</strong>
 * <ol>
 *   <li><em>BFF layer</em> (primary): checks with IDs that are directly available in
 *       the request (path variables, request-body fields). When a parent-scope ID is
 *       absent the check degrades to "has-any role of this type", which blocks pure
 *       Customers while allowing any holder of the required role class through.
 *   <li><em>Microservice layer</em> (defence-in-depth): fetches the resource from DB,
 *       then re-checks with fully-scoped IDs. This is the authoritative gate.
 * </ol>
 *
 * <p>All methods return {@code false} (deny) when the principal is not a
 * {@link UserDetail}. {@code SuperAdmin (SA)} always wins.
 */
public class RbacGuard {

    // ── Tenant-level ─────────────────────────────────────────────────────────

    /**
     * SA || TA(tenantId).
     * Used for tenant edit / delete operations and branch creation.
     */
    public boolean isTenantAdmin(Authentication auth, String tenantId) {
        UserDetail u = resolveUser(auth);
        return u != null && u.isTenantAdmin(tenantId);
    }

    // ── Branch-level ─────────────────────────────────────────────────────────

    /**
     * SA || TA(tenantId) || BA(branchId).
     * Used for branch edit / delete and service creation / edit / delete.
     * Pass {@code null} for either ID when it is not available in the request;
     * the check then degrades to "has any TA/BA role".
     */
    public boolean isBranchManageable(Authentication auth, String branchId, String tenantId) {
        UserDetail u = resolveUser(auth);
        return u != null && (u.isBranchAdmin(branchId) || u.isTenantAdmin(tenantId));
    }

    // ── Service / slot level ──────────────────────────────────────────────────

    /**
     * SA || TA(tenantId) || BA(branchId) || SM(serviceId).
     * Used for slot create / edit / delete and queue lifecycle management.
     * Pass {@code null} for IDs that are absent; degrades to "has any of that role".
     */
    public boolean isSlotManageable(Authentication auth,
                                    String serviceId, String branchId, String tenantId) {
        UserDetail u = resolveUser(auth);
        return u != null && (u.isServiceManager(serviceId)
                || u.isBranchAdmin(branchId)
                || u.isTenantAdmin(tenantId));
    }

    // ── Queue token operations ────────────────────────────────────────────────

    /**
     * SA || TA(any) || BA(any) || SM(any) || ST(queueId).
     * Used for call-next and mark-complete.  When the higher-level scope IDs
     * (tenantId, branchId, serviceId) are unavailable at BFF, the check uses
     * "has any of that role type".  The microservice will enforce the fully
     * scoped check after fetching the queue record.
     */
    public boolean canOperateQueue(Authentication auth, String queueId) {
        UserDetail u = resolveUser(auth);
        if (u == null) return false;
        // null tenantId/branchId/serviceId → hasRole("TA"/"BA"/"SM") — "any" fallback
        return u.canOperateQueue(queueId, null, null, null);
    }

    // ── Catch-all for write operations where only the resource ID is in the path ──

    /**
     * True when the user holds at least one of SA / TA / BA / SM / ST.
     * Used by BFF update/delete endpoints where the parent-scope ID is not in the
     * request (e.g., {@code DELETE /services/{serviceId}}).  The downstream
     * microservice re-validates with the full scope.
     */
    public boolean hasManagementRole(Authentication auth) {
        UserDetail u = resolveUser(auth);
        if (u == null) return false;
        return u.isSuperAdmin()
                || u.hasRole("TA")
                || u.hasRole("BA")
                || u.hasRole("SM")
                || u.hasRole("ST");
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private UserDetail resolveUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof UserDetail ud) ? ud : null;
    }
}
