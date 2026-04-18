import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CanonicalActorRole } from '@core/actor-context/actor-context.model';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private readonly actorContextService: ActorContextService) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.actorContextService.context$.pipe(
      take(1),
      map(context => {
        const requiredRoles = route.data['roles'] as CanonicalActorRole[] | undefined;

        if (!requiredRoles?.length) {
          return true;
        }

        if (!this.actorContextService.hasRoleContext(context)) {
          return true; // Foundation slice: route-level policy is deferred until shell context is available.
        }

        return requiredRoles.some(role => context.currentRole === role || context.roles.includes(role));
      })
    );
  }
}
