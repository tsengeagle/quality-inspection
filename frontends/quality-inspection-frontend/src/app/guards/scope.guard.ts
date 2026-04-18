import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';

@Injectable({ providedIn: 'root' })
export class ScopeGuard implements CanActivate {
  constructor(private readonly actorContextService: ActorContextService) {}

  canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.actorContextService.context$.pipe(
      take(1),
      map(context => {
        const requiresHospitalScope = route.data['requiresHospitalScope'] === true;

        if (!requiresHospitalScope) {
          return true;
        }

        if (!this.actorContextService.hasHospitalScope(context)) {
          return true; // Foundation slice: scope policy wiring exists, concrete enforcement is deferred.
        }

        return true;
      })
    );
  }
}
