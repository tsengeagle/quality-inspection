package com.tzuchi.his.quality.inspection.application.common;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable value object representing the authenticated actor making a request.
 * Passed as the first parameter to every use case execute() method.
 */
public class ActorContext {

    private final String userId;
    private final String userName;
    private final Set<DomainRole> roles;

    public ActorContext(String userId, String userName, Set<DomainRole> roles) {
        this.userId = userId;
        this.userName = userName;
        this.roles = (roles != null && !roles.isEmpty())
                ? EnumSet.copyOf(roles)
                : EnumSet.noneOf(DomainRole.class);
    }

    public void requireRole(DomainRole required) {
        if (!roles.contains(required)) {
            throw new UnauthorizedException(
                    "Actor '" + userId + "' does not have required role: " + required);
        }
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Set<DomainRole> getRoles() { return Collections.unmodifiableSet(roles); }
}
