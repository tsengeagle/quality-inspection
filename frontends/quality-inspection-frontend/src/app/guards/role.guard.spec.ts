import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { ActivatedRouteSnapshot } from '@angular/router';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { ActorContext } from '@core/actor-context/actor-context.model';
import { RoleGuard } from './role.guard';

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
}

describe('RoleGuard', () => {
  it('allows a route when the canonical actor role satisfies route data', async () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['HOSPITAL_MANAGER'],
      currentRole: 'HOSPITAL_MANAGER',
      hospitalId: 'H-001'
    });
    const guard = new RoleGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { roles: ['HOSPITAL_MANAGER'] };

    await expectAsync(firstValueFrom(guard.canActivate(route, {} as never))).toBeResolvedTo(true);
  });

  it('denies a route when the actor does not satisfy the required roles', async () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['REVIEWER'],
      currentRole: 'REVIEWER',
      hospitalId: 'H-001'
    });
    const guard = new RoleGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { roles: ['HOSPITAL_MANAGER'] };

    await expectAsync(firstValueFrom(guard.canActivate(route, {} as never))).toBeResolvedTo(false);
  });

  it('keeps the foundation fallback open when role context is not available yet', async () => {
    const actorContextService = new ActorContextServiceStub();
    const guard = new RoleGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { roles: ['SYSTEM_ADMIN'] };

    await expectAsync(firstValueFrom(guard.canActivate(route, {} as never))).toBeResolvedTo(true);
  });

  it('applies the same policy to child routes', async () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['HOSPITAL_MANAGER'],
      currentRole: 'HOSPITAL_MANAGER',
      hospitalId: 'H-001'
    });
    const guard = new RoleGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { roles: ['HOSPITAL_MANAGER'] };

    await expectAsync(firstValueFrom(guard.canActivateChild(route, {} as never))).toBeResolvedTo(true);
  });
});
