import { CanonicalActorRole } from '@core/actor-context/actor-context.model';

export type AccessPermission =
  | 'VIEW_SAMPLING_BATCH_LIST'
  | 'VIEW_SAMPLING_BATCH_METRICS'
  | 'VIEW_SAMPLING_BATCH_CASES'
  | 'RETRY_SAMPLING_BATCH'
  | 'EXECUTE_SAMPLING'
  | 'VIEW_CASE_ASSIGNMENT_PAGE';

export type AccessScope =
  | 'GLOBAL_METADATA'
  | 'HOSPITAL'
  | 'AUTHORIZED_HOSPITALS'
  | 'SELF_ASSIGNED'
  | 'SELF_SUBMITTED';

export type AccessProjectionType = 'FULL' | 'METADATA_ONLY';

export type AccessPage = 'UI_006_SAMPLING_EXECUTE' | 'UI_007_BATCH_LIST' | 'UI_011_CASE_ASSIGNMENT';

export interface AccessCheck {
  page?: AccessPage;
  anyRoles?: CanonicalActorRole[];
  anyPermissions?: AccessPermission[];
  allPermissions?: AccessPermission[];
  requiredScopes?: AccessScope[];
  projectionType?: AccessProjectionType;
}

export interface AccessProfile {
  currentRole: CanonicalActorRole | null;
  actorRoles: CanonicalActorRole[];
  permissions: AccessPermission[];
  scopes: AccessScope[];
  projectionType: AccessProjectionType;
  hospitalId: string | null;
  userId: string | null;
  isResolved: boolean;
}

export interface AccessPagePolicy extends Omit<AccessCheck, 'page'> {}

export const ACCESS_PAGE_POLICIES: Record<AccessPage, AccessPagePolicy> = {
  UI_006_SAMPLING_EXECUTE: {
    anyPermissions: ['EXECUTE_SAMPLING'],
    requiredScopes: ['HOSPITAL'],
    projectionType: 'FULL'
  },
  UI_007_BATCH_LIST: {
    anyPermissions: ['VIEW_SAMPLING_BATCH_LIST']
  },
  UI_011_CASE_ASSIGNMENT: {
    anyPermissions: ['VIEW_CASE_ASSIGNMENT_PAGE'],
    requiredScopes: ['HOSPITAL'],
    projectionType: 'FULL'
  }
};

export const ROLE_PERMISSION_MAP: Record<CanonicalActorRole, AccessPermission[]> = {
  SYSTEM_ADMIN: ['VIEW_SAMPLING_BATCH_LIST'],
  HOSPITAL_MANAGER: [
    'VIEW_SAMPLING_BATCH_LIST',
    'VIEW_SAMPLING_BATCH_METRICS',
    'VIEW_SAMPLING_BATCH_CASES',
    'RETRY_SAMPLING_BATCH',
    'EXECUTE_SAMPLING',
    'VIEW_CASE_ASSIGNMENT_PAGE'
  ],
  AUDITOR: [
    'VIEW_SAMPLING_BATCH_LIST',
    'VIEW_SAMPLING_BATCH_METRICS',
    'VIEW_SAMPLING_BATCH_CASES'
  ],
  REREVIEWER: [],
  REVIEWER: []
};

export const ROLE_SCOPE_MAP: Record<CanonicalActorRole, AccessScope[]> = {
  SYSTEM_ADMIN: ['GLOBAL_METADATA'],
  HOSPITAL_MANAGER: ['HOSPITAL'],
  AUDITOR: ['AUTHORIZED_HOSPITALS'],
  REREVIEWER: ['SELF_ASSIGNED', 'SELF_SUBMITTED'],
  REVIEWER: ['SELF_ASSIGNED', 'SELF_SUBMITTED']
};
