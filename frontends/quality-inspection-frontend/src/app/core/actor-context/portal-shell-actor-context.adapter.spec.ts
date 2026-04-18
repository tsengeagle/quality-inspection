import { PortalShellActorContextAdapter } from './portal-shell-actor-context.adapter';
import {
  PortalShellEventBusPort,
  TOPIC_CURR_ROLE,
  TOPIC_MFE_READY,
  TOPIC_USER_INFO,
  TOPIC_USER_ROLES
} from './portal-shell-event-bus.token';

class FakeEventBusPort implements PortalShellEventBusPort {
  readonly handlers = new Map<string, Array<(payload: unknown) => void>>();
  readonly publishedTopics: string[] = [];

  publish(topic: string): void {
    this.publishedTopics.push(topic);
  }

  subscribe(topic: string, handler: (payload: unknown) => void): void {
    const existingHandlers = this.handlers.get(topic) ?? [];
    existingHandlers.push(handler);
    this.handlers.set(topic, existingHandlers);
  }

  emit(topic: string, payload: unknown): void {
    for (const handler of this.handlers.get(topic) ?? []) {
      handler(payload);
    }
  }
}

describe('PortalShellActorContextAdapter', () => {
  it('publishes MFE_READY and aggregates shell topics as they arrive', () => {
    const eventBusPort = new FakeEventBusPort();
    const adapter = new PortalShellActorContextAdapter(eventBusPort);
    const states: string[] = [];

    adapter.rawState$.subscribe(state => {
      states.push(JSON.stringify(state));
    });

    adapter.initialize();
    eventBusPort.emit(TOPIC_USER_INFO, { userId: 'U-001', userName: 'Alice', hospitalId: 'H-001' });
    eventBusPort.emit(TOPIC_USER_ROLES, ['MANAGER', 'REVIEWER']);
    eventBusPort.emit(TOPIC_CURR_ROLE, 'REVIEWER');

    expect(eventBusPort.publishedTopics).toEqual([TOPIC_MFE_READY]);
    expect(states.at(-1)).toBe(JSON.stringify({
      userInfo: { userId: 'U-001', userName: 'Alice', hospitalId: 'H-001' },
      userRoles: ['MANAGER', 'REVIEWER'],
      currentRole: 'REVIEWER'
    }));
  });

  it('accepts partial topic arrival without assuming synchronous completeness', () => {
    const eventBusPort = new FakeEventBusPort();
    const adapter = new PortalShellActorContextAdapter(eventBusPort);
    let latestState = '';

    adapter.rawState$.subscribe(state => {
      latestState = JSON.stringify(state);
    });

    adapter.initialize();
    eventBusPort.emit(TOPIC_USER_INFO, { userId: 'U-001' });

    expect(latestState).toBe(JSON.stringify({
      userInfo: { userId: 'U-001', userName: null, hospitalId: null },
      userRoles: [],
      currentRole: null
    }));
  });
});
