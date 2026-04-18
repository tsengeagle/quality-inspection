import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';

import { RawShellContextState } from './actor-context.model';

export interface ActorContextSource {
  readonly rawState$: Observable<RawShellContextState>;
  initialize(): void;
}

export const ACTOR_CONTEXT_SOURCE = new InjectionToken<ActorContextSource>('ACTOR_CONTEXT_SOURCE');
