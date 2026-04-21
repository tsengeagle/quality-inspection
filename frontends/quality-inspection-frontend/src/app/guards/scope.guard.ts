import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { AccessControlService } from '@shared/access/access-control.service';
import { AccessPage } from '@shared/access/access-control.model';

@Injectable({ providedIn: 'root' })
export class ScopeGuard implements CanActivate, CanActivateChild {
  constructor(
    private readonly actorContextService: ActorContextService,
    private readonly accessControlService: AccessControlService
  ) {}

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
        const accessPage = route.data['accessPage'] as AccessPage | undefined;
        const requiresHospitalScope = route.data['requiresHospitalScope'] === true;

        if (!requiresHospitalScope && !accessPage) {
          return true;
        }

        if (!this.accessControlService.isResolvedContext(context)) {
          return false;
        }

        if (accessPage) {
          return this.accessControlService.canAccess({ page: accessPage }, context);
        }

        return this.actorContextService.hasHospitalScope(context);
      })
    );
  }
}
