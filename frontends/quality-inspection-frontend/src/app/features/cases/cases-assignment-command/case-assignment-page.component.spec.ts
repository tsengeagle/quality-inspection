import { FormsModule } from '@angular/forms';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';

import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import {
  AssignmentCandidateViewModel,
  AssignmentContextViewModel,
  CasesService
} from '../services/cases.service';
import { CaseAssignmentPageComponent } from './case-assignment-page.component';

describe('CaseAssignmentPageComponent', () => {
  let fixture: ComponentFixture<CaseAssignmentPageComponent>;
  let component: CaseAssignmentPageComponent;
  let casesService: jasmine.SpyObj<CasesService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    casesService = jasmine.createSpyObj<CasesService>('CasesService', [
      'getAssignmentContext',
      'getAssignmentCandidates',
      'assignReviewer',
      'reassignReviewer',
      'assignRereviewer',
      'reassignRereviewer'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    casesService.getAssignmentContext.and.returnValue(of(buildAssignmentContext()));
    casesService.getAssignmentCandidates.and.callFake((_: string, role: 'REVIEWER' | 'REREVIEWER') => {
      return of(role === 'REVIEWER' ? buildReviewerCandidates() : buildRereviewerCandidates());
    });
    casesService.assignReviewer.and.returnValue(of({ caseId: 'CASE-011' }));
    casesService.reassignReviewer.and.returnValue(of({ caseId: 'CASE-011' }));
    casesService.assignRereviewer.and.returnValue(of({ caseId: 'CASE-011' }));
    casesService.reassignRereviewer.and.returnValue(of({ caseId: 'CASE-011' }));

    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [
        CaseAssignmentPageComponent,
        CaseSummaryHeaderComponent,
        ProjectionNoticeBannerComponent,
        StatusOrBlockedBannerComponent,
        ModeIndicatorStripComponent
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ caseId: 'CASE-011' })
            }
          }
        },
        {
          provide: CasesService,
          useValue: casesService
        },
        {
          provide: Router,
          useValue: router
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseAssignmentPageComponent);
    component = fixture.componentInstance;
  });

  function buildAssignmentContext(overrides: Partial<AssignmentContextViewModel> = {}): AssignmentContextViewModel {
    return {
      caseId: 'CASE-011',
      projectId: 'PROJECT-001',
      batchId: 'BATCH-001',
      hospitalId: 'H001',
      medicalRecordType: 'OUTPATIENT',
      status: 'SELECTED_PENDING_ASSIGNMENT',
      pageMode: 'ASSIGN_INITIAL',
      blocked: false,
      blockedReason: null,
      allowedActions: ['ASSIGN_REVIEWER'],
      currentReviewer: undefined,
      currentRereviewer: undefined,
      reviewerDueAt: '2026-04-20T12:00:00',
      rereviewerDueAt: undefined,
      effectiveDueAt: '2026-04-20T12:00:00',
      ...overrides
    };
  }

  function buildReviewerCandidates(): AssignmentCandidateViewModel[] {
    return [
      {
        userId: 'reviewer-1',
        displayName: 'Dr. Reviewer One',
        displayLabel: 'reviewer-1 | Dr. Reviewer One',
        role: 'REVIEWER',
        active: true,
        selectable: true,
        blockedReason: null
      },
      {
        userId: 'reviewer-inactive',
        displayName: 'Dr. Inactive',
        displayLabel: 'reviewer-inactive | Dr. Inactive',
        role: 'REVIEWER',
        active: false,
        selectable: false,
        blockedReason: 'CANDIDATE_INACTIVE'
      }
    ];
  }

  function buildRereviewerCandidates(): AssignmentCandidateViewModel[] {
    return [
      {
        userId: 'rereviewer-1',
        displayName: 'Dr. ReReviewer One',
        displayLabel: 'rereviewer-1 | Dr. ReReviewer One',
        role: 'REREVIEWER',
        active: true,
        selectable: true,
        blockedReason: null
      },
      {
        userId: 'reviewer-1',
        displayName: 'Dr. Reviewer One',
        displayLabel: 'reviewer-1 | Dr. Reviewer One',
        role: 'REREVIEWER',
        active: true,
        selectable: false,
        blockedReason: 'REREVIEWER_SAME_AS_REVIEWER'
      }
    ];
  }

  it('loads backend assignment context and candidate truth for initial assignment mode', () => {
    fixture.detectChanges();

    expect(component.mode).toBe('ASSIGN_INITIAL');
    expect(casesService.getAssignmentContext).toHaveBeenCalledWith('CASE-011');
    expect(casesService.getAssignmentCandidates).toHaveBeenCalledWith('CASE-011', 'REVIEWER', '');
    expect(casesService.getAssignmentCandidates).toHaveBeenCalledWith('CASE-011', 'REREVIEWER', '');
    expect(fixture.nativeElement.querySelector('.reviewer-assignment-section')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.rereviewer-assignment-section')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('reviewer-1 | Dr. Reviewer One');
    expect(fixture.nativeElement.textContent).toContain('CANDIDATE_INACTIVE');
    expect(component.primaryActionLabel).toBe('初審指派');
    expect(component.primaryActionDisabled).toBeFalse();
  });

  it('renders mixed assignment mode with rereviewer candidate blocked reason', () => {
    casesService.getAssignmentContext.and.returnValue(of(buildAssignmentContext({
      status: 'PENDING_REREVIEW',
      pageMode: 'MIXED_ASSIGNMENT',
      allowedActions: ['ASSIGN_REREVIEWER'],
      currentReviewer: {
        userId: 'reviewer-1',
        displayName: 'Dr. Reviewer One',
        displayLabel: 'reviewer-1 | Dr. Reviewer One',
        assignedAt: '2026-04-18T09:00:00'
      },
      rereviewerDueAt: '2026-04-26T12:00:00',
      effectiveDueAt: '2026-04-26T12:00:00'
    })));

    fixture.detectChanges();

    expect(component.mode).toBe('MIXED_ASSIGNMENT');
    expect(fixture.nativeElement.querySelector('.rereviewer-assignment-section')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('REREVIEWER_SAME_AS_REVIEWER');
    expect(component.primaryActionLabel).toBe('複審指派');
  });

  it('shows blocked banner and hides submit zone when backend marks the case blocked', () => {
    casesService.getAssignmentContext.and.returnValue(of(buildAssignmentContext({
      status: 'CLOSED',
      pageMode: 'READONLY_BLOCKED',
      blocked: true,
      blockedReason: 'CASE_ALREADY_CLOSED',
      allowedActions: []
    })));

    fixture.detectChanges();

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="assignment-submit-zone"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('此案件已結案');
  });

  it('submits reviewer assignment and refetches assignment truth after success', () => {
    casesService.getAssignmentContext.and.returnValues(
      of(buildAssignmentContext()),
      of(buildAssignmentContext({
        status: 'PENDING_INITIAL_REVIEW',
        pageMode: 'REASSIGN_INITIAL',
        allowedActions: ['REASSIGN_REVIEWER'],
        currentReviewer: {
          userId: 'reviewer-1',
          displayName: 'Dr. Reviewer One',
          displayLabel: 'reviewer-1 | Dr. Reviewer One',
          assignedAt: '2026-04-18T09:00:00'
        }
      }))
    );

    fixture.detectChanges();
    component.onPrimaryAction();
    fixture.detectChanges();

    expect(casesService.assignReviewer).toHaveBeenCalledWith('CASE-011', {
      reviewerId: 'reviewer-1',
      dueAt: '2026-04-20T12:00:00'
    });
    expect(casesService.getAssignmentContext).toHaveBeenCalledTimes(2);
    expect(component.commandFeedbackMessage).toBe('初審指派已送出。');
    expect(component.mode).toBe('REASSIGN_INITIAL');
  });

  it('shows minimal command error feedback when assignment command fails', () => {
    casesService.assignReviewer.and.returnValue(throwError(() => new Error('assign failed')));

    fixture.detectChanges();
    component.onPrimaryAction();
    fixture.detectChanges();

    expect(component.commandFeedbackMessage).toBe('案件分派執行失敗，請確認指派資訊後再試。');
    expect(component.commandFeedbackTone).toBe('blocked');
  });

  it('navigates back to case detail', () => {
    fixture.detectChanges();

    component.onBackToCaseDetail();

    expect(router.navigate).toHaveBeenCalledWith(['/cases', 'CASE-011']);
  });
});
