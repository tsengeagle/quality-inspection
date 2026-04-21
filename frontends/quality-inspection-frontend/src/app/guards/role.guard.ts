import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CanonicalActorRole } from '@core/actor-context/actor-context.model';
import { AccessControlService } from '@shared/access/access-control.service';
import { AccessPage } from '@shared/access/access-control.model';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate, CanActivateChild {
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
        const requiredRoles = route.data['roles'] as CanonicalActorRole[] | undefined;

        if (!accessPage && !requiredRoles?.length) {
          return true;
        }

        if (!this.accessControlService.isResolvedContext(context)) {
          return false;
        }

        if (accessPage) {
          return this.accessControlService.canAccessPage(accessPage, context);
        }

        return this.accessControlService.hasAnyRole(requiredRoles ?? [], context);
      })
    );
  }
}
