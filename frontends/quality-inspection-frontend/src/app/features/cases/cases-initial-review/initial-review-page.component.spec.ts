import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

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
      'reviseInitialReview',
      'fixInitialReview'
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
      caseStatus: 'INITIAL_REVIEW_COMPLETED',
      status: 'SUBMITTED',
      totalScore: 90
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
    // D-054 第 3 點：提交後一律停在 INITIAL_REVIEW_COMPLETED，固定時才判斷是否進複審
    expect(fixture.nativeElement.textContent).toContain('初審已提交，尚未固定。確認結果無誤後，請按「固定初審」。');
    expect(fixture.nativeElement.textContent).not.toContain('送往複審');
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
      caseStatus: 'INITIAL_REVIEW_COMPLETED',
      status: 'SUBMITTED',
      totalScore: 90
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
    // D-054 第 3 點：提交後一律停在 INITIAL_REVIEW_COMPLETED，固定時才判斷是否進複審
    expect(fixture.nativeElement.textContent).toContain('初審已提交，尚未固定。確認結果無誤後，請按「固定初審」。');
    expect(fixture.nativeElement.textContent).not.toContain('送往複審');
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
  // ---- D-054 第 9 點：初審頁的「固定初審」按鈕 ----

  function revisionDetail(allowedActions: string[], status = 'INITIAL_REVIEW_COMPLETED'): CaseDetailViewModel {
    // 表單顯示的提交必須與固定目標（basedOnVersion）一致，按鈕才會啟用
    casesService.getSubmissionDetail.and.returnValue(of(revisionBaseSubmission()));
    return buildDetail({
      status,
      initialReviewContext: {
        pageMode: 'REVISION',
        hasDraft: false,
        hasSubmittedBase: true,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions,
        basedOnVersion: { submissionId: 'SUB-INITIAL-002', version: 2, status: 'SUBMITTED', submittedAt: '2026-04-15T09:00:00+08:00' }
      }
    });
  }

  function revisionBaseSubmission() {
    return buildSubmissionDetail({
      submissionId: 'SUB-INITIAL-002', revisionNo: 2, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-2', answerValue: 'N' }], comment: 'revision base'
    });
  }

  it('shows an enabled fix button when the backend allows FIX_INITIAL_REVIEW and fixes the based-on version', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    casesService.fixInitialReview.and.returnValue(of({
      caseId: 'CASE-012', caseStatus: 'PENDING_REREVIEW', submissionId: 'SUB-INITIAL-002', fixedAt: '2026-11-17T01:00:00Z'
    }));
    const loadsBefore = casesService.getCaseDetail.calls.count();

    const button = getButtonByText('固定初審');
    expect(button).toBeDefined();
    expect(button?.disabled).toBeFalse();
    button?.click();
    fixture.detectChanges();

    expect(casesService.fixInitialReview).toHaveBeenCalledWith('CASE-012', 'SUB-INITIAL-002');
    expect(fixture.nativeElement.textContent).toContain('初審已固定，案件已送往複審。');
    expect(casesService.getCaseDetail.calls.count()).toBe(loadsBefore + 1);
  });

  it('reports a fixed review that stays for the manager when the case does not go to rereview', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    casesService.fixInitialReview.and.returnValue(of({
      caseId: 'CASE-012', caseStatus: 'INITIAL_REVIEW_COMPLETED', submissionId: 'SUB-INITIAL-002', fixedAt: '2026-11-17T01:00:00Z'
    }));

    getButtonByText('固定初審')?.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('初審已固定，案件不需複審，由管理者決定後續處理。');
  });

  it('disables the fix button when the backend does not allow FIX_INITIAL_REVIEW', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW']));

    const button = getButtonByText('固定初審');
    expect(button?.disabled).toBeTrue();
    button?.click();
    expect(casesService.fixInitialReview).not.toHaveBeenCalled();
  });

  it('disables the fix button while the form has unsubmitted changes', () => {
    casesService.getSubmissionDetail.and.returnValue(of(buildSubmissionDetail({
      submissionId: 'SUB-INITIAL-002', revisionNo: 2, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-2', answerValue: 'N' }]
    })));
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));

    setInputValue('[data-testid="initial-review-answer-input-0"]', 'Y');

    const button = getButtonByText('固定初審');
    expect(button?.disabled).toBeTrue();
    button?.click();
    expect(casesService.fixInitialReview).not.toHaveBeenCalled();
  });

  it('reloads the page when fixing fails, e.g. an already fixed review (D-054 第 7 點)', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    casesService.fixInitialReview.and.returnValue(throwError(() => ({ status: 409 })));
    const loadsBefore = casesService.getCaseDetail.calls.count();

    getButtonByText('固定初審')?.click();
    fixture.detectChanges();

    expect(casesService.getCaseDetail.calls.count()).toBe(loadsBefore + 1);
    expect(fixture.nativeElement.textContent).toContain('固定初審失敗，已重新整理頁面。');
  });

  it('shows that the initial review is fixed when the backend blocks with INITIAL_REVIEW_FIXED', () => {
    render(buildDetail({
      status: 'PENDING_REREVIEW',
      initialReviewContext: {
        pageMode: 'READONLY_BLOCKED',
        hasDraft: false,
        hasSubmittedBase: true,
        readonly: true,
        blocked: true,
        blockedReason: 'INITIAL_REVIEW_FIXED',
        scoreSheetVersionId: 'SS-001',
        allowedActions: []
      }
    }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.textContent).toContain('初審已固定，不可再修改。');
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).toBeNull();
  });
  // ---- PR #56 審查：固定目標必須是畫面上已載入的版本；命令與重新載入期間鎖定表單 ----

  it('keeps the fix button disabled until the based-on submission has loaded', () => {
    const load$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    const detail = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    casesService.getSubmissionDetail.and.returnValue(load$);
    render(detail);

    expect(getButtonByText('固定初審')?.disabled).toBeTrue();

    load$.next(revisionBaseSubmission());
    fixture.detectChanges();
    expect(getButtonByText('固定初審')?.disabled).toBeFalse();
  });

  it('keeps the fix button disabled when the based-on submission fails to load', () => {
    const detail = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    casesService.getSubmissionDetail.and.returnValue(throwError(() => ({ status: 500 })));
    render(detail);

    const button = getButtonByText('固定初審');
    expect(button?.disabled).toBeTrue();
    button?.click();
    expect(casesService.fixInitialReview).not.toHaveBeenCalled();
  });

  it('disables the fix button while a newer based-on version is still loading (screen shows the old one)', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    expect(getButtonByText('固定初審')?.disabled).toBeFalse();

    const newer$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newer$);
    const newer = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    casesService.getSubmissionDetail.and.returnValue(newer$);
    newer.initialReviewContext!.basedOnVersion = { submissionId: 'SUB-INITIAL-003', version: 3, status: 'SUBMITTED' };
    component.applyCaseDetail(newer);
    fixture.detectChanges();

    expect(getButtonByText('固定初審')?.disabled).toBeTrue();
    getButtonByText('固定初審')?.click();
    expect(casesService.fixInitialReview).not.toHaveBeenCalled();

    newer$.next(buildSubmissionDetail({ submissionId: 'SUB-INITIAL-003', revisionNo: 3, status: 'SUBMITTED' }));
    fixture.detectChanges();
    expect(getButtonByText('固定初審')?.disabled).toBeFalse();
  });

  it('locks the form while the fix command and the following reload are in flight', async () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    const fix$ = new Subject<{ caseId: string; caseStatus: string; submissionId: string; fixedAt: string }>();
    const reload$ = new Subject<CaseDetailViewModel>();
    casesService.fixInitialReview.and.returnValue(fix$);
    casesService.getCaseDetail.and.returnValue(reload$);

    const answer = () => fixture.nativeElement.querySelector('[data-testid="initial-review-answer-input-0"]') as HTMLInputElement;
    const comment = () => fixture.nativeElement.querySelector('[data-testid="initial-review-comment-input"]') as HTMLTextAreaElement;
    expect(answer().disabled).toBeFalse();

    getButtonByText('固定初審')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(answer().disabled).withContext('固定請求進行中').toBeTrue();
    expect(comment().disabled).withContext('固定請求進行中').toBeTrue();

    fix$.next({ caseId: 'CASE-012', caseStatus: 'PENDING_REREVIEW', submissionId: 'SUB-INITIAL-002', fixedAt: '2026-11-17T01:00:00Z' });
    fix$.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(answer().disabled).withContext('重新載入進行中').toBeTrue();
    expect(getButtonByText('固定初審')?.disabled).withContext('重新載入進行中').toBeTrue();

    reload$.next(revisionDetail(['REVISE_INITIAL_REVIEW']));
    reload$.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(answer().disabled).withContext('重新載入完成').toBeFalse();
  });
  // ---- 比照 PR #58（複審頁）：案件詳情重新載入完成後，提交明細仍在載入時，表單與命令按鈕須持續鎖定 ----

  it('keeps the form and revise button locked until the new submission detail has loaded after a revise', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    casesService.reviseInitialReview.and.returnValue(of({ submissionId: 'SUB-INITIAL-003', revisionNo: 3 }));

    const newer = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    newer.initialReviewContext!.basedOnVersion = { submissionId: 'SUB-INITIAL-003', version: 3, status: 'SUBMITTED' };
    const newerSubmission$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newerSubmission$);
    casesService.getCaseDetail.and.returnValue(of(newer));

    getButtonByText('提交初審修正')?.click();
    fixture.detectChanges();

    expect(component.reloadPending).withContext('案件詳情已載入完成').toBeFalse();
    expect(component.formLocked).withContext('提交明細仍在載入').toBeTrue();
    expect(getButtonByText('提交初審修正')?.disabled).withContext('提交明細仍在載入').toBeTrue();
    expect(getButtonByText('固定初審')?.disabled).withContext('提交明細仍在載入').toBeTrue();
    getButtonByText('提交初審修正')?.click();
    expect(casesService.reviseInitialReview).toHaveBeenCalledTimes(1);

    newerSubmission$.next(buildSubmissionDetail({
      submissionId: 'SUB-INITIAL-003', revisionNo: 3, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-3', answerValue: 'Y' }], comment: 'newer'
    }));
    newerSubmission$.complete();
    fixture.detectChanges();

    expect(component.formLocked).withContext('提交明細載入完成').toBeFalse();
    expect(getButtonByText('提交初審修正')?.disabled).withContext('提交明細載入完成').toBeFalse();
    expect(component.formComment).toBe('newer');
  });

  it('unlocks the form when the submission detail fails to load, while keeping the fix button disabled', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    expect(component.formLocked).toBeFalse();

    const newer = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    newer.initialReviewContext!.basedOnVersion = { submissionId: 'SUB-INITIAL-003', version: 3, status: 'SUBMITTED' };
    casesService.getSubmissionDetail.and.returnValue(throwError(() => ({ status: 500 })));
    component.applyCaseDetail(newer);
    fixture.detectChanges();

    expect(component.submissionLoadPending).toBeFalse();
    expect(component.formLocked).toBeFalse();
    expect(getButtonByText('固定初審')?.disabled).toBeTrue();
  });

  it('does not unlock when an older submission load resolves while a newer one is still pending', () => {
    const older$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    const detail = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    casesService.getSubmissionDetail.and.returnValue(older$);
    render(detail);
    expect(component.formLocked).toBeTrue();

    const newer = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    newer.initialReviewContext!.basedOnVersion = { submissionId: 'SUB-INITIAL-003', version: 3, status: 'SUBMITTED' };
    const newer$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newer$);
    component.applyCaseDetail(newer);
    fixture.detectChanges();

    older$.next(revisionBaseSubmission());
    older$.complete();
    fixture.detectChanges();
    expect(component.formLocked).withContext('較舊請求已回應，較新請求仍在載入').toBeTrue();
    expect(getButtonByText('提交初審修正')?.disabled).withContext('較新請求仍在載入').toBeTrue();

    newer$.next(buildSubmissionDetail({ submissionId: 'SUB-INITIAL-003', revisionNo: 3, status: 'SUBMITTED' }));
    fixture.detectChanges();
    expect(component.formLocked).toBeFalse();
    expect(getButtonByText('提交初審修正')?.disabled).toBeFalse();
    expect(getButtonByText('固定初審')?.disabled).toBeFalse();
  });

  it('keeps save-draft and submit disabled until the draft submission detail has loaded (avoids saving an empty form)', () => {
    const draft$ = new Subject<ReturnType<typeof buildSubmissionDetail>>();
    casesService.getSubmissionDetail.and.returnValue(draft$);
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

    expect(getButtonByText('保存草稿')?.disabled).toBeTrue();
    expect(getButtonByText('提交初審')?.disabled).toBeTrue();
    getButtonByText('保存草稿')?.click();
    getButtonByText('提交初審')?.click();
    expect(casesService.saveInitialReviewDraft).not.toHaveBeenCalled();
    expect(casesService.submitInitialReview).not.toHaveBeenCalled();

    draft$.next(buildSubmissionDetail());
    fixture.detectChanges();
    expect(getButtonByText('保存草稿')?.disabled).toBeFalse();
    expect(getButtonByText('提交初審')?.disabled).toBeFalse();
  });

  // ---- 被取代的提交明細載入：較晚回應不得覆蓋表單或留下鎖定 ----

  it('releases the lock when a pending submission load is superseded by a page with no form source, and ignores its late response', () => {
    const detail = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    const stale$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(stale$);
    render(detail);
    expect(component.formLocked).withContext('A 載入中').toBeTrue();

    component.applyCaseDetail(buildDetail());
    fixture.detectChanges();
    expect(component.mode).toBe('NEW');
    expect(component.submissionLoadPending).withContext('改為無表單來源的頁面').toBeFalse();
    expect(component.formLocked).withContext('改為無表單來源的頁面').toBeFalse();
    expect(getButtonByText('開始初審')?.disabled).withContext('依 allowedActions').toBeFalse();

    stale$.next(revisionBaseSubmission());
    stale$.complete();
    fixture.detectChanges();
    expect(component.formAnswers).withContext('A 的較晚回應').toEqual([]);
    expect(component.formComment).withContext('A 的較晚回應').toBe('');
    expect(component.formSourceSubmissionId).withContext('A 的較晚回應').toBeUndefined();
  });

  it('releases the lock when the page returns to an already loaded submission while a newer load is pending, and ignores the newer late response', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    expect(component.formSourceSubmissionId).toBe('SUB-INITIAL-002');

    const newer = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    newer.initialReviewContext!.basedOnVersion = { submissionId: 'SUB-INITIAL-003', version: 3, status: 'SUBMITTED' };
    const newer$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newer$);
    component.applyCaseDetail(newer);
    fixture.detectChanges();
    expect(component.formLocked).withContext('B 載入中').toBeTrue();

    component.applyCaseDetail(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    fixture.detectChanges();
    expect(component.submissionLoadPending).withContext('回到已載入的 A').toBeFalse();
    expect(component.formLocked).withContext('回到已載入的 A').toBeFalse();
    expect(getButtonByText('提交初審修正')?.disabled).withContext('回到已載入的 A').toBeFalse();
    expect(getButtonByText('固定初審')?.disabled).withContext('回到已載入的 A').toBeFalse();

    newer$.next(buildSubmissionDetail({
      submissionId: 'SUB-INITIAL-003', revisionNo: 3, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-3', answerValue: 'Y' }], comment: 'stale newer'
    }));
    newer$.complete();
    fixture.detectChanges();
    expect(component.formSourceSubmissionId).withContext('B 的較晚回應').toBe('SUB-INITIAL-002');
    expect(component.formComment).withContext('B 的較晚回應').toBe('revision base');
    expect(component.formAnswers).withContext('B 的較晚回應').toEqual([{ itemId: 'ITEM-2', answerValue: 'N' }]);
  });

  it('ignores a late error from a superseded submission load (no feedback, no form reset)', () => {
    const detail = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    const stale$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(stale$);
    render(detail);

    component.applyCaseDetail(buildDetail());
    fixture.detectChanges();

    stale$.error({ status: 500 });
    fixture.detectChanges();
    expect(component.commandFeedbackMessage).withContext('A 的較晚錯誤').toBe('');
    expect(component.formSourceSubmissionId).withContext('A 的較晚錯誤').toBeUndefined();
    expect(component.formStateLoaded).withContext('A 的較晚錯誤').toBeTrue();
    expect(component.formLocked).toBeFalse();
    expect(getButtonByText('開始初審')?.disabled).toBeFalse();
  });
  // ---- 同一筆提交重複載入：以請求序號判斷，較舊請求的回應不得提早解鎖或寫入表單 ----

  it('stays locked until the latest of two loads for the same submission responds', () => {
    render(revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']));
    // revisionDetail() 會重設 getSubmissionDetail 的回傳值，延遲回應須在建好兩份 detail 之後才指定
    const first = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    const second = revisionDetail(['REVISE_INITIAL_REVIEW', 'FIX_INITIAL_REVIEW']);
    for (const detail of [first, second]) {
      detail.initialReviewContext!.basedOnVersion = { submissionId: 'SUB-INITIAL-003', version: 3, status: 'SUBMITTED' };
    }
    const newerSubmission = () => buildSubmissionDetail({
      submissionId: 'SUB-INITIAL-003', revisionNo: 3, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-3', answerValue: 'Y' }], comment: 'newer'
    });
    const first$ = new Subject<ReturnType<typeof newerSubmission>>();
    const second$ = new Subject<ReturnType<typeof newerSubmission>>();
    casesService.getSubmissionDetail.and.returnValues(first$, second$);

    component.applyCaseDetail(first);
    fixture.detectChanges();
    component.applyCaseDetail(second);
    fixture.detectChanges();
    expect(casesService.getSubmissionDetail).toHaveBeenCalledTimes(3);

    first$.next(newerSubmission());
    first$.complete();
    fixture.detectChanges();
    expect(component.formLocked).withContext('同一筆的較新請求仍在載入').toBeTrue();
    expect(getButtonByText('提交初審修正')?.disabled).withContext('同一筆的較新請求仍在載入').toBeTrue();

    second$.next(newerSubmission());
    second$.complete();
    fixture.detectChanges();
    expect(component.formLocked).toBeFalse();
    expect(getButtonByText('提交初審修正')?.disabled).toBeFalse();
    expect(component.formSourceSubmissionId).toBe('SUB-INITIAL-003');
  });
});
