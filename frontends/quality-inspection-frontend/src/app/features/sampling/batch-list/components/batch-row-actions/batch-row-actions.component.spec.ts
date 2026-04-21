import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';

import { ActorContext } from '@core/actor-context/actor-context.model';
import { ActorContextService } from '@core/actor-context/actor-context.service';
import { SharedModule } from '@shared/shared.module';

import { BatchRowActionsComponent } from './batch-row-actions.component';

class ActorContextServiceStub {
  private readonly subject = new BehaviorSubject<ActorContext>({
    userId: 'MGR-001',
    userName: 'Manager',
    roles: ['HOSPITAL_MANAGER'],
    currentRole: 'HOSPITAL_MANAGER',
    hospitalId: 'H-001'
  });

  readonly context$ = this.subject.asObservable();

  setRole(role: ActorContext['currentRole'], hospitalId: string | null = 'H-001'): void {
    this.subject.next({
      userId: 'U-001',
      userName: 'Actor',
      roles: role ? [role] : [],
      currentRole: role,
      hospitalId
    });
  }

  getSnapshot(): ActorContext {
    return this.subject.getValue();
  }
}

describe('BatchRowActionsComponent', () => {
  let fixture: ComponentFixture<BatchRowActionsComponent>;
  let component: BatchRowActionsComponent;
  let actorContextService: ActorContextServiceStub;

  beforeEach(async () => {
    actorContextService = new ActorContextServiceStub();

    await TestBed.configureTestingModule({
      declarations: [BatchRowActionsComponent],
      imports: [SharedModule],
      providers: [
        { provide: ActorContextService, useValue: actorContextService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BatchRowActionsComponent);
    component = fixture.componentInstance;
    component.batch = {
      batchId: 'BATCH-001',
      batchName: '失敗批次',
      projectId: 'PRJ-001',
      projectName: 'Project',
      projectVersionNumber: 3,
      status: 'FAILED',
      executionMode: 'MANUAL',
      populationCount: 12,
      eligibleCount: 10,
      excludedCount: 2,
      selectedCount: 5,
      createdAt: '2026-04-20T10:00:00+08:00',
      finishedAt: '2026-04-20T10:15:00+08:00',
      retryOfBatchId: null,
      canRetry: true
    };
  });

  it('shows retry and view-cases for manager on retryable failed batches', () => {
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('查看案件');
    expect(text).toContain('重跑');
  });

  it('hides retry but keeps view-cases for auditor', () => {
    actorContextService.setRole('AUDITOR', null);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('查看案件');
    expect(text).not.toContain('重跑');
  });

  it('hides business actions for metadata-only actors', () => {
    actorContextService.setRole('SYSTEM_ADMIN', null);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('查看案件');
    expect(text).not.toContain('重跑');
  });
});