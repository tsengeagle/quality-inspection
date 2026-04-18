import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';

@Injectable({ providedIn: 'root' })
export class ScopeGuard implements CanActivate, CanActivateChild {
  constructor(private readonly actorContextService: ActorContextService) {}

  canActivate(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<boolean> {
    return this.evaluateRoute(route);
  }

  canActivateChild(childRoute: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Observable<boolean> {
    return this.evaluateRoute(childRoute);
  }

  private evaluateRoute(route: ActivatedRouteSnapshot): Observable<boolean> {
    return this.actorContextService.context$.pipe(
      take(1),
      map(context => {
        const requiresHospitalScope = route.data['requiresHospitalScope'] === true;

        if (!requiresHospitalScope) {
          return true;
        }

        const hasResolvedActorSnapshot = this.actorContextService.hasRoleContext(context)
          || context.userId !== null
          || context.userName !== null
          || context.hospitalId !== null;

        if (!hasResolvedActorSnapshot) {
          return true; // Foundation slice: scope policy wiring exists, concrete enforcement is deferred.
        }

        return this.actorContextService.hasHospitalScope(context);
      })
    );
  }
}
