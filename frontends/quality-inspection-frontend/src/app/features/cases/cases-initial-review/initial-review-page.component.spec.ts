import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { ReviewCommandBarComponent } from '../components/review-command-bar/review-command-bar.component';
import { ReviewFormSummaryStripComponent } from '../components/review-form-summary-strip/review-form-summary-strip.component';
import { ReviewVersionSummaryRegionComponent } from '../components/review-version-summary-region/review-version-summary-region.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { CaseDetailViewModel, CasesService } from '../services/cases.service';
import { InitialReviewPageComponent } from './initial-review-page.component';

describe('InitialReviewPageComponent', () => {
  let fixture: ComponentFixture<InitialReviewPageComponent>;
  let component: InitialReviewPageComponent;
  let casesService: jasmine.SpyObj<CasesService>;

  beforeEach(async () => {
    casesService = jasmine.createSpyObj<CasesService>('CasesService', [
      'getCaseDetail',
      'getSubmissionDetail',
      'startInitialReview',
      'saveInitialReviewDraft',
      'submitInitialReview',
      'reviseInitialReview'
    ]);

    await TestBed.configureTestingModule({
      declarations: [
        InitialReviewPageComponent,
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
              paramMap: convertToParamMap({ caseId: 'CASE-012' })
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InitialReviewPageComponent);
    component = fixture.componentInstance;
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
  });

  function buildDetail(overrides: Partial<CaseDetailViewModel> = {}): CaseDetailViewModel {
    return {
      caseId: 'CASE-012',
      status: 'PENDING_INITIAL_REVIEW',
      projectionType: 'FULL',
      actorRole: 'REVIEWER',
      isOwner: true,
      batchId: 'BATCH-001',
      hospitalId: 'H001',
      reviewerId: 'reviewer-1',
      rereviewerId: 'rereviewer-1',
      medicalRecordType: 'OUTPATIENT',
      sourceCaseKey: 'SK-012',
      submissions: [],
      initialReviewContext: {
        pageMode: 'NEW_INITIAL_REVIEW',
        hasDraft: false,
        hasSubmittedBase: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['START_INITIAL_REVIEW']
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
      submissionId: 'SUB-DRAFT-001',
      caseId: 'CASE-012',
      stage: 'INITIAL' as const,
      revisionNo: 1,
      status: 'DRAFT',
      reviewerId: 'reviewer-1',
      answers: [{ itemId: 'ITEM-1', answerValue: 'Y' }],
      comment: 'draft comment',
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

  it('covers NEW baseline and renders C15/C16 shared structure', () => {
    render(buildDetail());

    expect(component.mode).toBe('NEW');
    expect(fixture.nativeElement.querySelector('app-review-form-summary-strip')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-form-placeholder"]')).not.toBeNull();
    expect(getButtonByText('開始初審')?.disabled).toBeFalse();
  });

  it('starts initial review via backend command path and shows minimal success feedback', () => {
    casesService.getCaseDetail.and.returnValues(of(buildDetail()), of(buildDetail({
      status: 'INITIAL_REVIEW_IN_PROGRESS',
      initialReviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_INITIAL_REVIEW_DRAFT', 'SUBMIT_INITIAL_REVIEW'],
        currentSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' }
      }
    })));
    casesService.startInitialReview.and.returnValue(of({ submissionId: 'SUB-DRAFT-001' }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));

    fixture.detectChanges();
    fixture.detectChanges();
    getButtonByText('開始初審')?.click();
    fixture.detectChanges();

    expect(casesService.startInitialReview).toHaveBeenCalledWith('CASE-012');
    expect(fixture.nativeElement.textContent).toContain('已開始初審，系統已建立最小草稿。');
  });

  it('uses allowedActions to control draft and submit CTA for EDIT_DRAFT mode', () => {
    render(buildDetail({
      status: 'INITIAL_REVIEW_IN_PROGRESS',
      initialReviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_INITIAL_REVIEW_DRAFT'],
        currentSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' }
      }
    }));

    expect(component.mode).toBe('EDIT_DRAFT');
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-commentary-placeholder"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-attachment-placeholder"]')).not.toBeNull();
    expect(getButtonByText('保存草稿')?.disabled).toBeFalse();
    expect(getButtonByText('提交初審')?.disabled).toBeTrue();
  });

  it('saves initial-review draft using page form state as payload source', () => {
    render(buildDetail({
      status: 'INITIAL_REVIEW_IN_PROGRESS',
      initialReviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_INITIAL_REVIEW_DRAFT', 'SUBMIT_INITIAL_REVIEW'],
        currentSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' }
      }
    }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
    casesService.saveInitialReviewDraft.and.returnValue(of({ submissionId: 'SUB-DRAFT-001', status: 'DRAFT', stage: 'INITIAL', totalScore: 5 }));

    setInputValue('[data-testid="initial-review-answer-input-0"]', 'N');
    setInputValue('[data-testid="initial-review-comment-input"]', 'page state draft comment');

    getButtonByText('保存草稿')?.click();
    fixture.detectChanges();

    expect(casesService.getSubmissionDetail).toHaveBeenCalledWith('SUB-DRAFT-001');
    expect(casesService.saveInitialReviewDraft).toHaveBeenCalledWith('CASE-012', {
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'page state draft comment'
    });
    expect(fixture.nativeElement.textContent).toContain('初審草稿已保存。');
  });

  it('shows already up to date when the initial draft has no changes (D-044)', () => {
    render(buildDetail({
      status: 'INITIAL_REVIEW_IN_PROGRESS',
      initialReviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_INITIAL_REVIEW_DRAFT', 'SUBMIT_INITIAL_REVIEW'],
        currentSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' }
      }
    }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
    casesService.saveInitialReviewDraft.and.returnValue(of({ noChanges: true }));

    setInputValue('[data-testid="initial-review-answer-input-0"]', 'N');
    setInputValue('[data-testid="initial-review-comment-input"]', 'page state draft comment');

    getButtonByText('保存草稿')?.click();
    fixture.detectChanges();

    expect(casesService.getSubmissionDetail).toHaveBeenCalledWith('SUB-DRAFT-001');
    expect(casesService.saveInitialReviewDraft).toHaveBeenCalledWith('CASE-012', {
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'page state draft comment'
    });
    expect(fixture.nativeElement.textContent).toContain('草稿已是最新，沒有需要保存的變更。');
    expect(fixture.nativeElement.textContent).not.toContain('初審草稿已保存。');
  });

  it('submits initial review after persisting page form state into draft payload', () => {
    render(buildDetail({
      status: 'INITIAL_REVIEW_IN_PROGRESS',
      initialReviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_INITIAL_REVIEW_DRAFT', 'SUBMIT_INITIAL_REVIEW'],
        currentSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' }
      }
    }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
    casesService.saveInitialReviewDraft.and.returnValue(of({ submissionId: 'SUB-DRAFT-001', status: 'DRAFT', stage: 'INITIAL', totalScore: 5 }));
    casesService.submitInitialReview.and.returnValue(of({
      submissionId: 'SUB-DRAFT-001',
      caseId: 'CASE-012',
      caseStatus: 'PENDING_REREVIEW',
      status: 'SUBMITTED',
      totalScore: 90,
      rereviewRequired: true
    }));

    setInputValue('[data-testid="initial-review-answer-input-0"]', 'N');
    setInputValue('[data-testid="initial-review-comment-input"]', 'submit from page state');

    getButtonByText('提交初審')?.click();
    fixture.detectChanges();

    expect(casesService.saveInitialReviewDraft).toHaveBeenCalledWith('CASE-012', {
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'submit from page state'
    });
    expect(casesService.submitInitialReview).toHaveBeenCalledWith('CASE-012');
    expect(fixture.nativeElement.textContent).toContain('初審已提交，案件已送往複審。');
  });

  it('still submits when the draft saved before submit has no changes (D-044)', () => {
    render(buildDetail({
      status: 'INITIAL_REVIEW_IN_PROGRESS',
      initialReviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_INITIAL_REVIEW_DRAFT', 'SUBMIT_INITIAL_REVIEW'],
        currentSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' },
        editableSubmission: { submissionId: 'SUB-DRAFT-001', version: 1, status: 'DRAFT' }
      }
    }));
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail()));
    casesService.saveInitialReviewDraft.and.returnValue(of({ noChanges: true }));
    casesService.submitInitialReview.and.returnValue(of({
      submissionId: 'SUB-DRAFT-001',
      caseId: 'CASE-012',
      caseStatus: 'PENDING_REREVIEW',
      status: 'SUBMITTED',
      totalScore: 90,
      rereviewRequired: true
    }));

    setInputValue('[data-testid="initial-review-answer-input-0"]', 'N');
    setInputValue('[data-testid="initial-review-comment-input"]', 'submit from page state');

    getButtonByText('提交初審')?.click();
    fixture.detectChanges();

    expect(casesService.saveInitialReviewDraft).toHaveBeenCalledWith('CASE-012', {
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'submit from page state'
    });
    expect(casesService.submitInitialReview).toHaveBeenCalledWith('CASE-012');
    expect(fixture.nativeElement.textContent).toContain('初審已提交，案件已送往複審。');
  });

  it('covers REVISION baseline, append-only copy, and C17 visibility', () => {
    render(buildDetail({
      status: 'INITIAL_REVIEW_COMPLETED',
      initialReviewContext: {
        pageMode: 'REVISION',
        hasDraft: false,
        hasSubmittedBase: true,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['REVISE_INITIAL_REVIEW'],
        basedOnVersion: { submissionId: 'SUB-INITIAL-002', version: 2, status: 'SUBMITTED', submittedAt: '2026-04-15T09:00:00+08:00' }
      }
    }));

    expect(component.mode).toBe('REVISION');
    expect(fixture.nativeElement.querySelector('app-review-version-summary-region')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('系統將建立新 revision，不會覆寫既有提交。');
  });

  it('revises initial review with payload sourced from page form state', () => {
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail({
      submissionId: 'SUB-INITIAL-002',
      revisionNo: 2,
      status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-2', answerValue: 'N' }],
      comment: 'revision base'
    })));
    render(buildDetail({
      status: 'INITIAL_REVIEW_COMPLETED',
      initialReviewContext: {
        pageMode: 'REVISION',
        hasDraft: false,
        hasSubmittedBase: true,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['REVISE_INITIAL_REVIEW'],
        basedOnVersion: { submissionId: 'SUB-INITIAL-002', version: 2, status: 'SUBMITTED', submittedAt: '2026-04-15T09:00:00+08:00' }
      }
    }));
    casesService.reviseInitialReview.and.returnValue(of({ submissionId: 'SUB-INITIAL-003', revisionNo: 3 }));

    setInputValue('[data-testid="initial-review-answer-input-0"]', 'Y');
    setInputValue('[data-testid="initial-review-comment-input"]', 'revision from page state');

    getButtonByText('提交初審修正')?.click();
    fixture.detectChanges();

    expect(casesService.reviseInitialReview).toHaveBeenCalledWith('CASE-012', 'SUB-INITIAL-002', {
      answers: [{ itemId: 'ITEM-2', answerValue: 'Y' }],
      comment: 'revision from page state'
    });
    expect(fixture.nativeElement.textContent).toContain('初審修正已提交。');
  });

  it('covers READONLY_BLOCKED baseline and shows form restriction grammar', () => {
    render(buildDetail({
      status: 'CLOSED',
      initialReviewContext: {
        pageMode: 'READONLY_BLOCKED',
        hasDraft: false,
        hasSubmittedBase: true,
        readonly: true,
        blocked: true,
        blockedReason: 'CASE_ALREADY_CLOSED',
        scoreSheetVersionId: 'SS-001',
        allowedActions: []
      }
    }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-readonly-guard"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).toBeNull();
  });
});
