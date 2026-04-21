import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { AccessControlService } from '@shared/access/access-control.service';

@Injectable({ providedIn: 'root' })
export class ProjectionGuard implements CanActivate, CanActivateChild {
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
        const metadataOnlyRoleRequired = route.data['metadataOnlyRoleRequired'] === true;

        if (!metadataOnlyRoleRequired) {
          return true;
        }

        if (!this.accessControlService.isResolvedContext(context)) {
          return false;
        }

        return this.accessControlService.canAccess({ projectionType: 'METADATA_ONLY' }, context);
      })
    );
  }
}
