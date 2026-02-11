package com.queuesetu.boot.core.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserDetail extends User {

    private final UUID userId;
    private final String email;
    private final List<List<String>> rolesPairs;

    public UserDetail(String username, String password,
                      Collection<? extends GrantedAuthority> authorities,
                      UUID userId, UUID branchId, UUID tenantId, String email) {
        super(username, password, authorities);
        this.userId = userId;
        this.email = email;
        this.rolesPairs = null;
    }

    public UserDetail(String username, String password,
                      Collection<? extends GrantedAuthority> authorities,
                      UUID userId, String email,
                      List<List<String>> rolesPairs) {
        super(username, password, authorities);
        this.userId = userId;
        this.email = email;
        this.rolesPairs = rolesPairs;
    }


    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public List<List<String>> getRolesPairs() { return rolesPairs; }

    public boolean hasRole(String role) {
        if (role == null || rolesPairs == null) return false;
        for (List<String> pair : rolesPairs) {
            if (pair == null || pair.isEmpty()) continue;
            String r = pair.get(0);
            if (r != null && r.equalsIgnoreCase(role)) return true;
        }
        return false;
    }

    public java.util.List<String> getIdsForRole(String role) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        if (role == null || rolesPairs == null) return ids;
        for (List<String> pair : rolesPairs) {
            if (pair == null || pair.size() < 2) continue;
            String r = pair.get(0);
            if (r != null && r.equalsIgnoreCase(role)) {
                String id = pair.get(1);
                if (id != null && !id.isBlank()) ids.add(id);
            }
        }
        return ids;
    }

    public java.util.List<java.util.UUID> getIdsForRoleAsUUIDs(String role) {
        java.util.List<java.util.UUID> ids = new java.util.ArrayList<>();
        for (String s : getIdsForRole(role)) {
            try {
                ids.add(java.util.UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ids;
    }

    public boolean isBranchAdmin(String branchId) {
        if (branchId == null || branchId.isBlank() || rolesPairs==null) return false;
        for (List<String> pair : rolesPairs) {
            if (pair == null || pair.size() < 2) continue;
            String r = pair.get(0);
            String id = pair.get(1);
            if ("BA".equalsIgnoreCase(r) && branchId.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTenantAdmin(String tenantId) {
        if (tenantId == null || tenantId.isBlank() || rolesPairs==null) return false;
        for (List<String> pair : rolesPairs) {
            if (pair == null || pair.size() < 2) continue;
            String r = pair.get(0);
            String id = pair.get(1);
            if ("TA".equalsIgnoreCase(r) && tenantId.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }
}
