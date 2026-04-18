import { APP_INITIALIZER, Provider } from '@angular/core';

import { environment } from '@env/environment';

import { ACTOR_CONTEXT_SOURCE, ActorContextSource } from './actor-context-source';
import { ActorContextService } from './actor-context.service';
import { PortalShellActorContextAdapter } from './portal-shell-actor-context.adapter';
import { PORTAL_SHELL_EVENT_BUS, PortalShellEventBusPort } from './portal-shell-event-bus.token';
import { StandaloneMockActorContextSource } from './standalone-mock-actor-context.source';

function createActorContextSource(eventBusPort: PortalShellEventBusPort | null): ActorContextSource {
  if (environment.standalone) {
    return new StandaloneMockActorContextSource();
  }

  return new PortalShellActorContextAdapter(eventBusPort);
}

function initializeActorContext(service: ActorContextService): () => void {
  return () => service.initialize();
}

export const ACTOR_CONTEXT_FOUNDATION_PROVIDERS: Provider[] = [
  {
    provide: PORTAL_SHELL_EVENT_BUS,
    useValue: null
  },
  {
    provide: ACTOR_CONTEXT_SOURCE,
    useFactory: createActorContextSource,
    deps: [PORTAL_SHELL_EVENT_BUS]
  },
  {
    provide: APP_INITIALIZER,
    useFactory: initializeActorContext,
    deps: [ActorContextService],
    multi: true
  }
];
