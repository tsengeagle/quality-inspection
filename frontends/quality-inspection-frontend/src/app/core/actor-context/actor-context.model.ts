export type CanonicalActorRole =
  | 'SYSTEM_ADMIN'
  | 'HOSPITAL_MANAGER'
  | 'AUDITOR'
  | 'REREVIEWER'
  | 'REVIEWER';

export interface ActorContext {
  userId: string | null;
  userName: string | null;
  roles: CanonicalActorRole[];
  currentRole: CanonicalActorRole | null;
  hospitalId: string | null;
}

export interface RawShellUserInfo {
  userId: string | null;
  userName: string | null;
  hospitalId: string | null;
}

export interface RawShellContextState {
  userInfo: RawShellUserInfo | null;
  userRoles: string[];
  currentRole: string | null;
}

export const EMPTY_ACTOR_CONTEXT: ActorContext = {
  userId: null,
  userName: null,
  roles: [],
  currentRole: null,
  hospitalId: null
};

export const EMPTY_RAW_SHELL_CONTEXT_STATE: RawShellContextState = {
  userInfo: null,
  userRoles: [],
  currentRole: null
};
