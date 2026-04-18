import { BehaviorSubject } from 'rxjs';

import { ActorContextSource } from './actor-context-source';
import {
  EMPTY_RAW_SHELL_CONTEXT_STATE,
  RawShellContextState,
  RawShellUserInfo
} from './actor-context.model';
import {
  PORTAL_SHELL_EVENT_BUS,
  PortalShellEventBusPort,
  TOPIC_CURR_ROLE,
  TOPIC_MFE_READY,
  TOPIC_USER_INFO,
  TOPIC_USER_ROLES
} from './portal-shell-event-bus.token';

export class PortalShellActorContextAdapter implements ActorContextSource {
  private readonly rawStateSubject = new BehaviorSubject<RawShellContextState>(EMPTY_RAW_SHELL_CONTEXT_STATE);
  private initialized = false;

  readonly rawState$ = this.rawStateSubject.asObservable();

  constructor(private readonly eventBusPort: PortalShellEventBusPort | null) {}

  initialize(): void {
    if (this.initialized) {
      return;
    }

    this.initialized = true;

    if (!this.eventBusPort) {
      return;
    }

    this.eventBusPort.subscribe(TOPIC_USER_INFO, payload => {
      this.patchState({ userInfo: extractUserInfo(payload) });
    });

    this.eventBusPort.subscribe(TOPIC_USER_ROLES, payload => {
      this.patchState({ userRoles: extractUserRoles(payload) });
    });

    this.eventBusPort.subscribe(TOPIC_CURR_ROLE, payload => {
      this.patchState({ currentRole: extractCurrentRole(payload) });
    });

    this.eventBusPort.publish(TOPIC_MFE_READY);
  }

  private patchState(patch: Partial<RawShellContextState>): void {
    this.rawStateSubject.next({
      ...this.rawStateSubject.getValue(),
      ...patch
    });
  }
}

function extractUserInfo(payload: unknown): RawShellUserInfo | null {
  if (!isRecord(payload)) {
    return null;
  }

  const userInfo: RawShellUserInfo = {
    userId: readString(payload, 'userId'),
    userName: readString(payload, 'userName'),
    hospitalId: readString(payload, 'hospitalId')
  };

  return userInfo.userId || userInfo.userName || userInfo.hospitalId ? userInfo : null;
}

function extractUserRoles(payload: unknown): string[] {
  if (Array.isArray(payload)) {
    return payload.filter((role): role is string => typeof role === 'string');
  }

  if (!isRecord(payload)) {
    return [];
  }

  const nestedRoles = payload['userRoles'];
  if (Array.isArray(nestedRoles)) {
    return nestedRoles.filter((role): role is string => typeof role === 'string');
  }

  const roles = payload['roles'];
  if (Array.isArray(roles)) {
    return roles.filter((role): role is string => typeof role === 'string');
  }

  return [];
}

function extractCurrentRole(payload: unknown): string | null {
  if (typeof payload === 'string') {
    return payload;
  }

  if (!isRecord(payload)) {
    return null;
  }

  return readString(payload, 'currRole')
    ?? readString(payload, 'currentRole')
    ?? readString(payload, 'role');
}

function readString(record: Record<string, unknown>, key: string): string | null {
  const value = record[key];
  return typeof value === 'string' && value.trim() ? value : null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object';
}

void PORTAL_SHELL_EVENT_BUS;
