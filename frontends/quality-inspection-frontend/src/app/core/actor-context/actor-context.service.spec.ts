import { BehaviorSubject } from 'rxjs';

import { ACTOR_CONTEXT_SOURCE, ActorContextSource } from './actor-context-source';
import { ActorContextService } from './actor-context.service';
import { RawShellContextState } from './actor-context.model';

class ActorContextSourceStub implements ActorContextSource {
  private readonly subject = new BehaviorSubject<RawShellContextState>({
    userInfo: null,
    userRoles: [],
    currentRole: null
  });

  readonly rawState$ = this.subject.asObservable();

  initialize(): void {
    // no-op
  }

  setState(state: RawShellContextState): void {
    this.subject.next(state);
  }
}

describe('ActorContextService', () => {
  it('resolves the primary role from currRole first and exposes a canonical snapshot', () => {
    const source = new ActorContextSourceStub();
    const service = new ActorContextService(source);

    service.initialize();
    source.setState({
      userInfo: {
        userId: 'U-001',
        userName: 'Alice',
        hospitalId: 'H-001'
      },
      userRoles: ['REVIEWER', 'MANAGER'],
      currentRole: 'manager'
    });

    expect(service.getSnapshot()).toEqual({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['REVIEWER', 'HOSPITAL_MANAGER'],
      currentRole: 'HOSPITAL_MANAGER',
      hospitalId: 'H-001'
    });
    expect(service.resolvePrimaryRole('REVIEWER')).toBe('HOSPITAL_MANAGER');
  });

  it('falls back to the supplied role when shell context is still empty', () => {
    const service = new ActorContextService(new ActorContextSourceStub());

    service.initialize();

    expect(service.resolvePrimaryRole('HOSPITAL_MANAGER')).toBe('HOSPITAL_MANAGER');
    expect(service.hasRoleContext()).toBeFalse();
    expect(service.hasHospitalScope()).toBeFalse();
  });
});
