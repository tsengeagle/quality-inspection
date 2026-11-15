import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { ReviewCommandBarComponent } from '../components/review-command-bar/review-command-bar.component';
import { ReviewFormSummaryStripComponent } from '../components/review-form-summary-strip/review-form-summary-strip.component';
import { ReviewVersionSummaryRegionComponent } from '../components/review-version-summary-region/review-version-summary-region.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { CaseDetailViewModel, CasesService } from '../services/cases.service';
import { RereviewPageComponent } from './rereview-page.component';

describe('RereviewPageComponent', () => {
  let fixture: ComponentFixture<RereviewPageComponent>;
  let component: RereviewPageComponent;
  let casesService: jasmine.SpyObj<CasesService>;

  beforeEach(async () => {
    casesService = jasmine.createSpyObj<CasesService>('CasesService', [
      'getCaseDetail',
      'getSubmissionDetail',
      'startRereview',
      'saveRereviewDraft',
      'submitRereview',
      'reviseRereview',
      'closeRereview'
    ]);

    await TestBed.configureTestingModule({
      declarations: [
        RereviewPageComponent,
        CaseSummaryHeaderComponent,
        ProjectionNoticeBannerComponent,
        StatusOrBlockedBannerComponent,
        ModeIndicatorStripComponent,
        ReviewFormSummaryStripComponent,
        ReviewCommandBarComponent,
        ReviewVersionSummaryRegionComponent
      ],
      imports: [FormsModule],
      providers: [
        { provide: CasesService, useValue: casesService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ caseId: 'CASE-013' })
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RereviewPageComponent);
    component = fixture.componentInstance;
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
  });

  function buildDetail(overrides: Partial<CaseDetailViewModel> = {}): CaseDetailViewModel {
    return {
      caseId: 'CASE-013',
      status: 'PENDING_REREVIEW',
      projectionType: 'FULL',
      actorRole: 'REREVIEWER',
      isOwner: true,
      batchId: 'BATCH-001',
      hospitalId: 'H001',
      reviewerId: 'reviewer-1',
      rereviewerId: 'rereviewer-1',
      medicalRecordType: 'OUTPATIENT',
      sourceCaseKey: 'SK-013',
      submissions: [],
      rereviewContext: {
        pageMode: 'NEW_REREVIEW',
        hasDraft: false,
        hasSubmittedBase: false,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['START_REREVIEW'],
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'PENDING_FINAL_DECISION'
      },
      ...overrides
    };
  }

  function render(detail: CaseDetailViewModel): void {
    casesService.getCaseDetail.and.returnValue(of(detail));
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function buildSubmissionDetail(overrides: Record<string, unknown> = {}) {
    return {
      submissionId: 'SUB-REREVIEW-001',
      caseId: 'CASE-013',
      stage: 'REREVIEW' as const,
      revisionNo: 1,
      status: 'DRAFT',
      reviewerId: 'rereviewer-1',
      answers: [{ itemId: 'ITEM-1', answerValue: 'Y' }],
      comment: 'draft comment',
      totalScore: 88,
      maskedFields: [],
      ...overrides
    };
  }

  function getButtons(): HTMLButtonElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('button'));
  }

  function getButtonByText(label: string): HTMLButtonElement | undefined {
    return getButtons().find(button => button.textContent?.includes(label));
  }

  function setInputValue(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement | HTMLTextAreaElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('covers NEW baseline with C18/C19 skeleton and rereview form placeholder', () => {
    render(buildDetail());

    expect(component.mode).toBe('NEW');
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-initial-summary-reference"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-final-decision-summary"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-form-placeholder"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Dr. Initial');
    expect(getButtonByText('開始複審')?.disabled).toBeFalse();
  });

  it('starts rereview via backend command path and shows minimal success feedback', () => {
    casesService.getCaseDetail.and.returnValues(of(buildDetail()), of(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_REREVIEW_DRAFT', 'SUBMIT_REREVIEW'],
        currentSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'PENDING_FINAL_DECISION'
      }
    })));
    casesService.startRereview.and.returnValue(of({ caseId: 'CASE-013' }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));

    fixture.detectChanges();
    fixture.detectChanges();
    getButtonByText('開始複審')?.click();
    fixture.detectChanges();

    expect(casesService.startRereview).toHaveBeenCalledWith('CASE-013');
    expect(fixture.nativeElement.textContent).toContain('已開始複審，系統已建立最小草稿。');
  });

  it('uses allowedActions to control draft and submit CTA for rereview EDIT_DRAFT mode', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_REREVIEW_DRAFT'],
        currentSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'PENDING_FINAL_DECISION'
      }
    }));

    expect(component.mode).toBe('EDIT_DRAFT');
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).not.toBeNull();
    expect(getButtonByText('保存複審草稿')?.disabled).toBeFalse();
    expect(getButtonByText('提交複審')?.disabled).toBeTrue();
  });

  it('saves rereview draft using page form state as payload source', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_REREVIEW_DRAFT', 'SUBMIT_REREVIEW'],
        currentSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'PENDING_FINAL_DECISION'
      }
    }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
    casesService.saveRereviewDraft.and.returnValue(of({ submissionId: 'SUB-REREVIEW-001', status: 'DRAFT', stage: 'REREVIEW', totalScore: 88 }));

    setInputValue('[data-testid="rereview-answer-input-0"]', 'N');
    setInputValue('[data-testid="rereview-comment-input"]', 'draft from page state');
    setInputValue('[data-testid="rereview-total-score-input"]', '91');

    getButtonByText('保存複審草稿')?.click();
    fixture.detectChanges();

    expect(casesService.saveRereviewDraft).toHaveBeenCalledWith('CASE-013', {
      submissionId: 'SUB-REREVIEW-001',
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'draft from page state',
      totalScore: 91
    });
    expect(fixture.nativeElement.textContent).toContain('複審草稿已保存。');
  });

  it('shows already up to date when the rereview draft has no changes (D-044)', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_REREVIEW_DRAFT', 'SUBMIT_REREVIEW'],
        currentSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'PENDING_FINAL_DECISION'
      }
    }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
    casesService.saveRereviewDraft.and.returnValue(of({ noChanges: true }));

    setInputValue('[data-testid="rereview-answer-input-0"]', 'N');
    setInputValue('[data-testid="rereview-comment-input"]', 'draft from page state');
    setInputValue('[data-testid="rereview-total-score-input"]', '91');

    getButtonByText('保存複審草稿')?.click();
    fixture.detectChanges();

    expect(casesService.saveRereviewDraft).toHaveBeenCalledWith('CASE-013', {
      submissionId: 'SUB-REREVIEW-001',
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'draft from page state',
      totalScore: 91
    });
    expect(fixture.nativeElement.textContent).toContain('草稿已是最新，沒有需要保存的變更。');
    expect(fixture.nativeElement.textContent).not.toContain('複審草稿已保存。');
  });

  it('submits rereview with page form state while leaving totalScore out of submit payload', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_REREVIEW_DRAFT', 'SUBMIT_REREVIEW'],
        currentSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'DRAFT' },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'PENDING_FINAL_DECISION'
      }
    }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail({ comment: 'submit comment' })));
    casesService.submitRereview.and.returnValue(of({ submissionId: 'SUB-REREVIEW-001', caseId: 'CASE-013', caseStatus: 'REREVIEW_IN_PROGRESS', status: 'SUBMITTED' }));

    setInputValue('[data-testid="rereview-answer-input-0"]', 'N');
    setInputValue('[data-testid="rereview-comment-input"]', 'submit from page state');
    setInputValue('[data-testid="rereview-total-score-input"]', '95');

    getButtonByText('提交複審')?.click();
    fixture.detectChanges();

    expect(casesService.submitRereview).toHaveBeenCalledWith('CASE-013', {
      submissionId: 'SUB-REREVIEW-001',
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'submit from page state'
    });
    // D-032／D-053：提交不結案；D-033：不顯示 finalScore
    expect(fixture.nativeElement.textContent).toContain('複審已提交，案件尚未結案。確認結果無誤後，請按「結案」。');
    expect(fixture.nativeElement.textContent).not.toContain('最終分數');
  });

  it('covers REVISION baseline and keeps C17 visible', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'REVISION',
        hasDraft: false,
        hasSubmittedBase: true,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['REVISE_REREVIEW'],
        basedOnVersion: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'SUBMITTED', submittedAt: '2026-04-15T11:00:00+08:00' },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'PASS'
      }
    }));

    expect(component.mode).toBe('REVISION');
    expect(fixture.nativeElement.querySelector('app-review-version-summary-region')).not.toBeNull();
  });

  function revisionContext(allowedActions: string[]) {
    return {
      pageMode: 'REVISION' as const,
      hasDraft: false,
      hasSubmittedBase: true,
      closedRevisionException: false,
      readonly: false,
      blocked: false,
      blockedReason: null,
      scoreSheetVersionId: 'SS-001',
      allowedActions,
      basedOnVersion: { submissionId: 'SUB-REREVIEW-002', version: 2, status: 'SUBMITTED' },
      initialSubmissionId: 'SUB-INITIAL-001',
      initialReviewerId: 'reviewer-1',
      initialReviewerName: 'Dr. Initial',
      initialTotalScore: null,
      finalConclusion: null
    };
  }

  it('closes the case with the current submitted rereview when the backend allows it (D-053)', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: revisionContext(['REVISE_REREVIEW', 'CLOSE_REREVIEW'])
    }));
    casesService.closeRereview.and.returnValue(of({ caseId: 'CASE-013', caseStatus: 'CLOSED', submissionId: 'SUB-REREVIEW-002' }));

    const closeButton = getButtonByText('結案');
    expect(closeButton).toBeDefined();
    expect(closeButton?.disabled).toBeFalse();

    closeButton?.click();
    fixture.detectChanges();

    expect(casesService.closeRereview).toHaveBeenCalledWith('CASE-013', 'SUB-REREVIEW-002');
    expect(fixture.nativeElement.textContent).toContain('案件已結案，複審結果已固定。');
    expect(casesService.getCaseDetail).toHaveBeenCalledTimes(2);
  });

  it('disables the close button when the backend does not allow closing', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: revisionContext(['REVISE_REREVIEW'])
    }));

    expect(getButtonByText('結案')?.disabled).toBeTrue();
    getButtonByText('結案')?.click();
    expect(casesService.closeRereview).not.toHaveBeenCalled();
  });

  it('disables closing while the revision form has unsaved changes', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: revisionContext(['REVISE_REREVIEW', 'CLOSE_REREVIEW'])
    }));

    setInputValue('[data-testid="rereview-comment-input"]', 'edited but not submitted');

    expect(getButtonByText('結案')?.disabled).toBeTrue();
    getButtonByText('結案')?.click();
    expect(casesService.closeRereview).not.toHaveBeenCalled();
  });

  it('reloads the case when closing returns CASE_TERMINAL (D-053 point 6)', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: revisionContext(['REVISE_REREVIEW', 'CLOSE_REREVIEW'])
    }));
    casesService.closeRereview.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { success: false, error: { code: 'CASE_TERMINAL', message: 'Case is in terminal status' } }
    })));

    getButtonByText('結案')?.click();
    fixture.detectChanges();

    expect(casesService.getCaseDetail).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('案件已結案或已取消，畫面已重新整理。');
  });

  it('shows a closed case as read-only without any revision entry (D-032)', () => {
    render(buildDetail({
      status: 'CLOSED',
      rereviewContext: {
        ...revisionContext([]),
        pageMode: 'READONLY_BLOCKED',
        hasSubmittedBase: true,
        readonly: true,
        blocked: true,
        blockedReason: 'CASE_ALREADY_CLOSED'
      }
    }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.textContent).toContain('案件已結案，複審結果已固定。');
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-c16-placeholder-label"]')).toBeNull();
  });

  it('covers READONLY_BLOCKED baseline and hides the command bar', () => {
    render(buildDetail({
      status: 'CANCELLED',
      rereviewContext: {
        pageMode: 'READONLY_BLOCKED',
        hasDraft: false,
        hasSubmittedBase: false,
        closedRevisionException: false,
        readonly: true,
        blocked: true,
        blockedReason: 'CASE_ALREADY_CANCELLED',
        scoreSheetVersionId: 'SS-001',
        allowedActions: [],
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 79,
        finalConclusion: 'BLOCKED'
      }
    }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).toBeNull();
  });
});
