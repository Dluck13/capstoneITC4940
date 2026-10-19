package com.itsupportoperationsdashboard.capstoneProject.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authorization predicates for future ticket entry points (@PreAuthorize).
 * These do not create ticket workflows or expose the V1 persistence services.
 */
@Component("ticketAccess")
public class TicketAccess {
    public boolean canCreate(Authentication auth) {
        return hasRole(auth, "END_USER") || isSupport(auth);
    }
    public boolean canView(Authentication auth, Long requesterId) {
        return isSupport(auth) || (hasRole(auth, "END_USER")
                && currentUserId(auth).equals(requesterId));
    }
    public boolean canComment(Authentication auth, Long requesterId) {
        return canView(auth, requesterId);
    }
    public boolean canAssign(Authentication auth, Long technicianId) {
        return hasRole(auth, "ADMIN") || (hasRole(auth, "TECHNICIAN")
                && currentUserId(auth).equals(technicianId));
    }
    public boolean canUpdateStatus(Authentication auth) { return isSupport(auth); }
    public boolean canResolve(Authentication auth) { return isSupport(auth); }
    public boolean canClose(Authentication auth) { return hasRole(auth, "ADMIN"); }
    public boolean isSupport(Authentication auth) {
        return hasRole(auth, "TECHNICIAN") || hasRole(auth, "ADMIN");
    }
    public Long currentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppPrincipal principal)) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        return principal.profile().id();
    }
    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppPrincipal
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
