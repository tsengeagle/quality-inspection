import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';

@Injectable({ providedIn: 'root' })
export class ProjectionGuard implements CanActivate {
  constructor(private readonly actorContextService: ActorContextService) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.actorContextService.context$.pipe(
      take(1),
      map(context => {
        const metadataOnlyRoleRequired = route.data['metadataOnlyRoleRequired'] === true;

        if (!metadataOnlyRoleRequired) {
          return true;
        }

        if (!this.actorContextService.hasRoleContext(context)) {
          return true; // Foundation slice: projection truth still comes from page APIs, not actor context.
        }

        return context.currentRole === 'SYSTEM_ADMIN' || context.roles.includes('SYSTEM_ADMIN');
      })
    );
  }
}
