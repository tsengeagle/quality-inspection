import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { ActivatedRouteSnapshot } from '@angular/router';

import { ActorContext } from '@core/actor-context/actor-context.model';
import { ActorContextService } from '@core/actor-context/actor-context.service';
import { ScopeGuard } from './scope.guard';

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

  hasRoleContext(context: ActorContext): boolean {
    return context.currentRole !== null || context.roles.length > 0;
  }

  hasHospitalScope(context: ActorContext): boolean {
    return context.hospitalId !== null;
  }
}

describe('ScopeGuard', () => {
  it('allows a protected route when the actor has hospital scope', async () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['HOSPITAL_MANAGER'],
      currentRole: 'HOSPITAL_MANAGER',
      hospitalId: 'H-001'
    });
    const guard = new ScopeGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { requiresHospitalScope: true };

    await expectAsync(firstValueFrom(guard.canActivate(route, {} as never))).toBeResolvedTo(true);
  });

  it('denies a protected route when actor context is resolved but hospital scope is missing', async () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['HOSPITAL_MANAGER'],
      currentRole: 'HOSPITAL_MANAGER',
      hospitalId: null
    });
    const guard = new ScopeGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { requiresHospitalScope: true };

    await expectAsync(firstValueFrom(guard.canActivate(route, {} as never))).toBeResolvedTo(false);
  });
});
