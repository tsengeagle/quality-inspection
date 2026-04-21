import { Injectable } from '@angular/core';

import { ActorContext, CanonicalActorRole } from '@core/actor-context/actor-context.model';
import { ActorContextService } from '@core/actor-context/actor-context.service';

import {
  ACCESS_PAGE_POLICIES,
  AccessCheck,
  AccessPage,
  AccessProfile,
  AccessProjectionType,
  AccessScope,
  AccessPermission,
  ROLE_PERMISSION_MAP,
  ROLE_SCOPE_MAP
} from './access-control.model';

@Injectable({ providedIn: 'root' })
export class AccessControlService {
  constructor(private readonly actorContextService: ActorContextService) {}

  getCurrentProfile(): AccessProfile {
    return this.getProfile(this.actorContextService.getSnapshot());
  }

  getProfile(context: ActorContext): AccessProfile {
    const actorRoles = this.getDistinctRoles(context);
    const permissions = this.flattenMappedValues(actorRoles, ROLE_PERMISSION_MAP);
    const scopes = this.flattenMappedValues(actorRoles, ROLE_SCOPE_MAP);
    const projectionType = actorRoles.includes('SYSTEM_ADMIN') ? 'METADATA_ONLY' : 'FULL';

    return {
      currentRole: context.currentRole,
      actorRoles,
      permissions,
      scopes,
      projectionType,
      hospitalId: context.hospitalId,
      userId: context.userId,
      isResolved: this.isResolvedContext(context)
    };
  }

  hasAnyRole(roles: CanonicalActorRole[] | CanonicalActorRole, context: ActorContext = this.actorContextService.getSnapshot()): boolean {
    const expectedRoles = Array.isArray(roles) ? roles : [roles];
    const profile = this.getProfile(context);

    return expectedRoles.some(role => profile.actorRoles.includes(role));
  }

  hasAnyPermission(permissions: AccessPermission[] | AccessPermission, context: ActorContext = this.actorContextService.getSnapshot()): boolean {
    const expectedPermissions = Array.isArray(permissions) ? permissions : [permissions];
    const profile = this.getProfile(context);

    return expectedPermissions.some(permission => profile.permissions.includes(permission));
  }

  hasAllPermissions(permissions: AccessPermission[] | AccessPermission, context: ActorContext = this.actorContextService.getSnapshot()): boolean {
    const expectedPermissions = Array.isArray(permissions) ? permissions : [permissions];
    const profile = this.getProfile(context);

    return expectedPermissions.every(permission => profile.permissions.includes(permission));
  }

  hasScopes(scopes: AccessScope[] | AccessScope, context: ActorContext = this.actorContextService.getSnapshot()): boolean {
    const expectedScopes = Array.isArray(scopes) ? scopes : [scopes];
    const profile = this.getProfile(context);

    return expectedScopes.every(scope => profile.scopes.includes(scope));
  }

  getProjectionType(context: ActorContext = this.actorContextService.getSnapshot()): AccessProjectionType {
    return this.getProfile(context).projectionType;
  }

  canAccess(check: AccessCheck, context: ActorContext = this.actorContextService.getSnapshot()): boolean {
    const profile = this.getProfile(context);
    const effectiveCheck = this.expandPagePolicy(check.page, check);

    if (!profile.isResolved) {
      return false;
    }

    if (effectiveCheck.anyRoles?.length && !effectiveCheck.anyRoles.some(role => profile.actorRoles.includes(role))) {
      return false;
    }

    if (effectiveCheck.anyPermissions?.length && !effectiveCheck.anyPermissions.some(permission => profile.permissions.includes(permission))) {
      return false;
    }

    if (effectiveCheck.allPermissions?.length && !effectiveCheck.allPermissions.every(permission => profile.permissions.includes(permission))) {
      return false;
    }

    if (effectiveCheck.requiredScopes?.length && !effectiveCheck.requiredScopes.every(scope => profile.scopes.includes(scope))) {
      return false;
    }

    if (effectiveCheck.projectionType && effectiveCheck.projectionType !== profile.projectionType) {
      return false;
    }

    return true;
  }

  canAccessPage(page: AccessPage, context: ActorContext = this.actorContextService.getSnapshot()): boolean {
    return this.canAccess({ page }, context);
  }

  isResolvedContext(context: ActorContext = this.actorContextService.getSnapshot()): boolean {
    return context.currentRole !== null
      || context.roles.length > 0
      || context.userId !== null
      || context.userName !== null
      || context.hospitalId !== null;
  }

  private getDistinctRoles(context: ActorContext): CanonicalActorRole[] {
    const roles = context.currentRole ? [context.currentRole, ...context.roles] : context.roles;

    return Array.from(new Set(roles));
  }

  private flattenMappedValues<T extends string>(roles: CanonicalActorRole[], mapping: Record<CanonicalActorRole, T[]>): T[] {
    return Array.from(new Set(roles.flatMap(role => mapping[role] ?? [])));
  }

  private expandPagePolicy(page: AccessPage | undefined, check: AccessCheck): AccessCheck {
    if (!page) {
      return check;
    }

    const pagePolicy = ACCESS_PAGE_POLICIES[page];

    return {
      anyRoles: [...(pagePolicy.anyRoles ?? []), ...(check.anyRoles ?? [])],
      anyPermissions: [...(pagePolicy.anyPermissions ?? []), ...(check.anyPermissions ?? [])],
      allPermissions: [...(pagePolicy.allPermissions ?? []), ...(check.allPermissions ?? [])],
      requiredScopes: [...(pagePolicy.requiredScopes ?? []), ...(check.requiredScopes ?? [])],
      projectionType: check.projectionType ?? pagePolicy.projectionType
    };
  }
}