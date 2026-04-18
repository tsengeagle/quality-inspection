import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map, shareReplay } from 'rxjs/operators';

import { ACTOR_CONTEXT_SOURCE, ActorContextSource } from './actor-context-source';
import { ActorContext, CanonicalActorRole, EMPTY_ACTOR_CONTEXT } from './actor-context.model';
import { canonicalizeRole, normalizeActorContext } from './actor-role.resolver';

@Injectable({ providedIn: 'root' })
export class ActorContextService {
  private currentSnapshot: ActorContext = EMPTY_ACTOR_CONTEXT;

  readonly context$: Observable<ActorContext> = this.source.rawState$.pipe(
    map(rawState => normalizeActorContext(rawState)),
    distinctUntilChanged((previous, current) => JSON.stringify(previous) === JSON.stringify(current)),
    map(context => {
      this.currentSnapshot = context;
      return context;
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  constructor(@Inject(ACTOR_CONTEXT_SOURCE) private readonly source: ActorContextSource) {}

  initialize(): void {
    this.source.initialize();
    this.context$.subscribe();
  }

  getSnapshot(): ActorContext {
    return this.currentSnapshot;
  }

  resolvePrimaryRole(fallbackRole: CanonicalActorRole): CanonicalActorRole {
    return this.currentSnapshot.currentRole
      ?? this.currentSnapshot.roles[0]
      ?? fallbackRole;
  }

  toCanonicalRole(role: string | null | undefined): CanonicalActorRole | null {
    return canonicalizeRole(role);
  }

  isManagementRole(role: string | null | undefined): boolean {
    return this.toCanonicalRole(role) === 'HOSPITAL_MANAGER';
  }

  isMetadataOnlyRole(role: string | null | undefined): boolean {
    return this.toCanonicalRole(role) === 'SYSTEM_ADMIN';
  }

  hasRoleContext(context: ActorContext = this.currentSnapshot): boolean {
    return context.currentRole !== null || context.roles.length > 0;
  }

  hasHospitalScope(context: ActorContext = this.currentSnapshot): boolean {
    return context.hospitalId !== null;
  }
}
