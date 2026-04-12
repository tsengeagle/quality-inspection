package com.tzuchi.his.quality.inspection.application.common;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable value object representing the authenticated actor making a request.
 * Passed as the first parameter to every use case execute() method.
 */
public class ActorContext {

    private final String userId;
    private final String userName;
    private final Set<DomainRole> roles;
    private final Optional<String> hospitalId;

    public ActorContext(String userId, String userName, Set<DomainRole> roles,
                        Optional<String> hospitalId) {
        this.userId = userId;
        this.userName = userName;
        this.roles = (roles != null && !roles.isEmpty())
                ? EnumSet.copyOf(roles)
                : EnumSet.noneOf(DomainRole.class);
        this.hospitalId = (hospitalId != null) ? hospitalId : Optional.empty();
    }

    public void requireRole(DomainRole required) {
        if (!roles.contains(required)) {
            throw new RoleDeniedException(
                    "Actor '" + userId + "' does not have required role: " + required);
        }
    }

    public boolean hasRole(DomainRole role) {
        return roles.contains(role);
    }

    public void requireAnyRole(DomainRole... required) {
        for (DomainRole r : required) {
            if (roles.contains(r)) return;
        }
        throw new RoleDeniedException(
                "Actor '" + userId + "' does not have any of the required roles");
    }

    /**
     * Returns the actor's hospitalId, or throws AccessDeniedException if not present.
     */
    public String requireHospitalId() {
        return hospitalId.orElseThrow(() ->
                new AccessDeniedException("Hospital context required"));
    }

    /**
     * Verifies the actor's hospitalId matches the target. Throws AccessDeniedException on mismatch.
     */
    public void requireHospitalScope(String targetHospitalId) {
        String myHospitalId = requireHospitalId();
        if (!myHospitalId.equals(targetHospitalId)) {
            throw new AccessDeniedException("Hospital scope mismatch");
        }
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Set<DomainRole> getRoles() { return Collections.unmodifiableSet(roles); }
    public Optional<String> getHospitalId() { return hospitalId; }
}
