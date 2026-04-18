import {
  ActorContext,
  CanonicalActorRole,
  EMPTY_ACTOR_CONTEXT,
  RawShellContextState
} from './actor-context.model';

const ROLE_ALIASES: Record<string, CanonicalActorRole> = {
  MANAGER: 'HOSPITAL_MANAGER',
  ADMIN: 'SYSTEM_ADMIN',
  SYSADMIN: 'SYSTEM_ADMIN'
};

export const ROLE_PRIORITY_ORDER: CanonicalActorRole[] = [
  'SYSTEM_ADMIN',
  'HOSPITAL_MANAGER',
  'AUDITOR',
  'REREVIEWER',
  'REVIEWER'
];

export function canonicalizeRole(input: string | null | undefined): CanonicalActorRole | null {
  if (!input) {
    return null;
  }

  const normalized = input.trim().toUpperCase();
  if (!normalized) {
    return null;
  }

  const canonical = ROLE_ALIASES[normalized] ?? normalized;
  return isCanonicalActorRole(canonical) ? canonical : null;
}

export function canonicalizeRoles(inputs: readonly string[]): CanonicalActorRole[] {
  const roles = inputs
    .map(role => canonicalizeRole(role))
    .filter((role): role is CanonicalActorRole => role !== null);

  return Array.from(new Set(roles));
}

export function resolvePrimaryRole(currentRole: string | null | undefined,
                                   userRoles: readonly string[]): CanonicalActorRole | null {
  const canonicalCurrentRole = canonicalizeRole(currentRole);
  if (canonicalCurrentRole) {
    return canonicalCurrentRole;
  }

  const canonicalRoles = canonicalizeRoles(userRoles);
  return ROLE_PRIORITY_ORDER.find(role => canonicalRoles.includes(role)) ?? null;
}

export function normalizeActorContext(rawState: RawShellContextState): ActorContext {
  const roles = canonicalizeRoles(rawState.userRoles);
  return {
    ...EMPTY_ACTOR_CONTEXT,
    userId: rawState.userInfo?.userId ?? null,
    userName: rawState.userInfo?.userName ?? null,
    hospitalId: rawState.userInfo?.hospitalId ?? null,
    roles,
    currentRole: resolvePrimaryRole(rawState.currentRole, rawState.userRoles)
  };
}

function isCanonicalActorRole(value: string): value is CanonicalActorRole {
  return ROLE_PRIORITY_ORDER.includes(value as CanonicalActorRole);
}
