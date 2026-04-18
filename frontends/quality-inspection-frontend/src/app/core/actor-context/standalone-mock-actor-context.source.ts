import { BehaviorSubject } from 'rxjs';

import { ActorContextSource } from './actor-context-source';
import { RawShellContextState } from './actor-context.model';

const STANDALONE_MOCK_STATE: RawShellContextState = {
  userInfo: {
    userId: 'standalone-manager',
    userName: 'Standalone Manager',
    hospitalId: 'HOSPITAL-001'
  },
  userRoles: ['HOSPITAL_MANAGER'],
  currentRole: 'HOSPITAL_MANAGER'
};

export class StandaloneMockActorContextSource implements ActorContextSource {
  private readonly rawStateSubject = new BehaviorSubject<RawShellContextState>(STANDALONE_MOCK_STATE);

  readonly rawState$ = this.rawStateSubject.asObservable();

  initialize(): void {
    // Standalone mode uses a stable mock payload with the same normalized shape as integrated mode.
  }
}
