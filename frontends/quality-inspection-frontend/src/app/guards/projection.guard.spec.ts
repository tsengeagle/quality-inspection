import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { ActivatedRouteSnapshot } from '@angular/router';

import { ActorContext } from '@core/actor-context/actor-context.model';
import { ActorContextService } from '@core/actor-context/actor-context.service';
import { ProjectionGuard } from './projection.guard';

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

describe('ProjectionGuard', () => {
  it('allows metadata-only protected routes for SYSTEM_ADMIN', async () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['SYSTEM_ADMIN'],
      currentRole: 'SYSTEM_ADMIN',
      hospitalId: null
    });
    const guard = new ProjectionGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { metadataOnlyRoleRequired: true };

    await expectAsync(firstValueFrom(guard.canActivate(route, {} as never))).toBeResolvedTo(true);
  });

  it('denies metadata-only protected routes for non SYSTEM_ADMIN roles', async () => {
    const actorContextService = new ActorContextServiceStub();
    actorContextService.setContext({
      userId: 'U-001',
      userName: 'Alice',
      roles: ['HOSPITAL_MANAGER'],
      currentRole: 'HOSPITAL_MANAGER',
      hospitalId: 'H-001'
    });
    const guard = new ProjectionGuard(actorContextService as unknown as ActorContextService);
    const route = new ActivatedRouteSnapshot();
    route.data = { metadataOnlyRoleRequired: true };

    await expectAsync(firstValueFrom(guard.canActivate(route, {} as never))).toBeResolvedTo(false);
  });
});
