import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';

import { SamplingExecuteContext, SamplingService } from '../services/sampling.service';
import { SamplingExecutePageComponent } from './sampling-execute-page.component';

describe('SamplingExecutePageComponent', () => {
  let fixture: ComponentFixture<SamplingExecutePageComponent>;
  let component: SamplingExecutePageComponent;
  let router: jasmine.SpyObj<Router>;
  let samplingService: jasmine.SpyObj<SamplingService>;

  function buildContext(overrides: Partial<SamplingExecuteContext> = {}): SamplingExecuteContext {
    return {
      projectId: 'PROJECT-001',
      projectName: '2026 年第二季品質審查',
      hospitalId: 'HOSP-01',
      ruleSummary: '抽樣方式：STRATIFIED',
      previewReady: false,
      executing: false,
      executeFailed: false,
      executedBatchId: null,
      blockedReason: null,
      previewStats: null,
      postExecuteSummary: null,
      ...overrides
    };
  }

  beforeEach(async () => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    samplingService = jasmine.createSpyObj<SamplingService>('SamplingService', ['getExecuteContext']);
    samplingService.getExecuteContext.and.returnValue(of(buildContext()));

    await TestBed.configureTestingModule({
      declarations: [SamplingExecutePageComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ projectId: 'PROJECT-001' })
            }
          }
        },
        { provide: Router, useValue: router },
        { provide: SamplingService, useValue: samplingService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(SamplingExecutePageComponent);
    component = fixture.componentInstance;
  });

  it('resolves the six-mode state machine baseline', () => {
    fixture.detectChanges();

    const cases: Array<{ context: SamplingExecuteContext; expected: string }> = [
      { context: buildContext(), expected: 'READY_FOR_PREVIEW' },
      { context: buildContext({ previewReady: true, previewStats: { populationCount: 120, eligibleCount: 80, excludedCount: 40 } }), expected: 'PREVIEW_COMPLETED' },
      { context: buildContext({ previewReady: true, executing: true, previewStats: { populationCount: 120, eligibleCount: 80, excludedCount: 40 } }), expected: 'EXECUTING' },
      { context: buildContext({ executedBatchId: 'BATCH-COMPLETED-001', postExecuteSummary: { tone: 'success', title: '抽選完成', description: '已建立正式批次。', batchId: 'BATCH-COMPLETED-001' } }), expected: 'EXECUTE_SUCCESS' },
      { context: buildContext({ executeFailed: true, previewStats: { populationCount: 120, eligibleCount: 80, excludedCount: 40 }, postExecuteSummary: { tone: 'failed', title: '抽選失敗', description: '請重新預估後再試。', batchId: null } }), expected: 'EXECUTE_FAILED' },
      { context: buildContext({ blockedReason: 'PROJECT_CLOSED' }), expected: 'READONLY_BLOCKED' }
    ];

    for (const testCase of cases) {
      component.resolveMode(testCase.context);
      expect(component.mode).withContext(testCase.expected).toBe(testCase.expected as any);
    }
  });

  it('maps the four readonly blocked reasons into the blocked baseline', () => {
    fixture.detectChanges();

    const reasons = ['PROJECT_NOT_ACTIVE', 'COMPLETED_BATCH_EXISTS', 'PROJECT_CLOSED', 'PERMISSION_REVOKED'] as const;

    for (const reason of reasons) {
      component.resolveMode(buildContext({ blockedReason: reason }));
      expect(component.mode).toBe('READONLY_BLOCKED');
      expect(component.blockedReason).toBe(reason);
    }
  });

  it('shows preview stats and confirm zone in PREVIEW_COMPLETED', () => {
    fixture.detectChanges();
    component.context = buildContext({
      previewReady: true,
      previewStats: { populationCount: 120, eligibleCount: 80, excludedCount: 40 }
    });
    component.resolveMode(component.context);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-sampling-preview-stats')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.execute-confirm-zone')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-sampling-post-execute-region')).toBeNull();
  });

  it('keeps the executing baseline focused on frozen preview output', () => {
    fixture.detectChanges();
    component.context = buildContext({
      previewReady: true,
      executing: true,
      previewStats: { populationCount: 120, eligibleCount: 80, excludedCount: 40 }
    });
    component.resolveMode(component.context);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-sampling-preview-stats')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.executing-hint')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.execute-confirm-zone')).toBeNull();
  });

  it('shows the post execute region in EXECUTE_SUCCESS', () => {
    fixture.detectChanges();
    component.context = buildContext({
      executedBatchId: 'BATCH-COMPLETED-001',
      postExecuteSummary: {
        tone: 'success',
        title: '抽選完成',
        description: '已建立正式批次。',
        batchId: 'BATCH-COMPLETED-001'
      }
    });
    component.resolveMode(component.context);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-sampling-post-execute-region')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.post-execute-actions')).not.toBeNull();
  });

  it('opens the confirm modal from the execute trigger baseline', () => {
    fixture.detectChanges();
    component.context = buildContext({
      previewReady: true,
      previewStats: { populationCount: 120, eligibleCount: 80, excludedCount: 40 }
    });
    component.resolveMode(component.context);

    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.execute-btn') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(component.showConfirmModal).toBeTrue();
    expect(fixture.nativeElement.querySelector('app-sampling-execute-confirm-modal')).not.toBeNull();
  });
});