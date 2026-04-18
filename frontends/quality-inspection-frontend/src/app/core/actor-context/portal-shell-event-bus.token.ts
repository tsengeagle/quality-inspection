import { InjectionToken } from '@angular/core';

export const TOPIC_USER_INFO = 'TOPIC_USER_INFO';
export const TOPIC_USER_ROLES = 'TOPIC_USER_ROLES';
export const TOPIC_CURR_ROLE = 'TOPIC_CURR_ROLE';
export const TOPIC_MFE_READY = 'TOPIC_MFE_READY';

export interface PortalShellEventBusPort {
  publish(topic: string, payload?: unknown): void;
  subscribe(topic: string, handler: (payload: unknown) => void): { unsubscribe(): void } | void;
}

export const PORTAL_SHELL_EVENT_BUS = new InjectionToken<PortalShellEventBusPort | null>('PORTAL_SHELL_EVENT_BUS');
