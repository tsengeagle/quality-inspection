import { BehaviorSubject } from 'rxjs';

import { ActorContext } from '@core/actor-context/actor-context.model';
import { ActorContextService } from '@core/actor-context/actor-context.service';

import { AccessControlService } from './access-control.service';

class ActorContextServiceStub {
  private readonly subject = new BehaviorSubject<ActorContext>({
    userId: null,
    userName: null,
    roles: [],
    currentRole: null,
    hospitalId: null
  });

  readonly context$ = this.subject.asObservable();

  setContext(context: ActorContext): void {
    this.subject.next(context);
  }

  getSnapshot(): ActorContext {
    return this.subject.getValue();
  }
}

describe('AccessControlService', () => {
  it('builds metadata-only access for SYSTEM_ADMIN batch list access', () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'SYS-001',
      userName: 'Sys Admin',
      roles: ['SYSTEM_ADMIN'],
      currentRole: 'SYSTEM_ADMIN',
      hospitalId: null
    });
    const service = new AccessControlService(actorContextService as unknown as ActorContextService);

    expect(service.canAccessPage('UI_007_BATCH_LIST')).toBeTrue();
    expect(service.hasAnyPermission('VIEW_SAMPLING_BATCH_METRICS')).toBeFalse();
    expect(service.getProjectionType()).toBe('METADATA_ONLY');
  });

  it('allows manager-only hospital scoped pages when manager context is resolved', () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'MGR-001',
      userName: 'Manager',
      roles: ['HOSPITAL_MANAGER'],
      currentRole: 'HOSPITAL_MANAGER',
      hospitalId: 'H-001'
    });
    const service = new AccessControlService(actorContextService as unknown as ActorContextService);

    expect(service.canAccessPage('UI_006_SAMPLING_EXECUTE')).toBeTrue();
    expect(service.canAccessPage('UI_011_CASE_ASSIGNMENT')).toBeTrue();
    expect(service.hasAnyPermission('RETRY_SAMPLING_BATCH')).toBeTrue();
  });

  it('denies page access when actor context is unresolved', () => {
    const actorContextService = new ActorContextServiceStub();
    const service = new AccessControlService(actorContextService as unknown as ActorContextService);

    expect(service.canAccessPage('UI_007_BATCH_LIST')).toBeFalse();
  });
});