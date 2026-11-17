import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject, throwError } from 'rxjs';

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
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['START_REREVIEW'],
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
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

  it('never renders initial score, final score or final conclusion even if the backend sends them (D-033)', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        ...revisionContext(['REVISE_REREVIEW']),
        initialTotalScore: 79,
        finalScore: 84,
        finalConclusion: 'SECRET_CONCLUSION'
      } as unknown as CaseDetailViewModel['rereviewContext']
    }));

    const text: string = fixture.nativeElement.textContent;
    expect(text).not.toContain('79');
    expect(text).not.toContain('84');
    expect(text).not.toContain('SECRET_CONCLUSION');
    expect(text).not.toContain('最終分數');
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-final-decision-summary"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-initial-summary-reference"]')).not.toBeNull();
  });

  it('covers REVISION baseline and keeps C17 visible', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'REVISION',
        hasDraft: false,
        hasSubmittedBase: true,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['REVISE_REREVIEW'],
        basedOnVersion: { submissionId: 'SUB-REREVIEW-001', version: 1, status: 'SUBMITTED', submittedAt: '2026-04-15T11:00:00+08:00' },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
      }
    }));

    expect(component.mode).toBe('REVISION');
    expect(fixture.nativeElement.querySelector('app-review-version-summary-region')).not.toBeNull();
  });

  function revisionContext(allowedActions: string[]) {
    // 表單顯示的提交必須與結案目標（basedOnVersion）一致，結案按鈕才會啟用
    casesService.getSubmissionDetail.and.returnValue(of(revisionBaseSubmission()));
    return {
      pageMode: 'REVISION' as const,
      hasDraft: false,
      hasSubmittedBase: true,
      readonly: false,
      blocked: false,
      blockedReason: null,
      scoreSheetVersionId: 'SS-001',
      allowedActions,
      basedOnVersion: { submissionId: 'SUB-REREVIEW-002', version: 2, status: 'SUBMITTED' },
      initialSubmissionId: 'SUB-INITIAL-001',
      initialReviewerId: 'reviewer-1',
      initialReviewerName: 'Dr. Initial',
    };
  }

  function revisionBaseSubmission() {
    return buildSubmissionDetail({
      submissionId: 'SUB-REREVIEW-002', revisionNo: 2, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-2', answerValue: 'N' }], comment: 'revision base'
    });
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
        readonly: true,
        blocked: true,
        blockedReason: 'CASE_ALREADY_CANCELLED',
        scoreSheetVersionId: 'SS-001',
        allowedActions: [],
        initialSubmissionId: 'SUB-INITIAL-001',
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
      }
    }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).toBeNull();
  });

  // ---- 與 PR #56 相同的保護：結案目標必須是畫面上已載入的版本；命令與重新載入期間鎖定表單 ----

  function revisionDetail(allowedActions: string[]): CaseDetailViewModel {
    return buildDetail({ status: 'REREVIEW_IN_PROGRESS', rereviewContext: revisionContext(allowedActions) });
  }

  it('keeps the close button disabled until the based-on submission has loaded', () => {
    const load$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    const detail = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    casesService.getSubmissionDetail.and.returnValue(load$);
    render(detail);

    expect(getButtonByText('結案')?.disabled).toBeTrue();
    getButtonByText('結案')?.click();
    expect(casesService.closeRereview).not.toHaveBeenCalled();

    load$.next(revisionBaseSubmission());
    fixture.detectChanges();
    expect(getButtonByText('結案')?.disabled).toBeFalse();
  });

  it('keeps the close button disabled when the based-on submission fails to load', () => {
    const detail = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    casesService.getSubmissionDetail.and.returnValue(throwError(() => ({ status: 500 })));
    render(detail);

    const button = getButtonByText('結案');
    expect(button?.disabled).toBeTrue();
    button?.click();
    expect(casesService.closeRereview).not.toHaveBeenCalled();
  });

  it('disables the close button while a newer based-on version is still loading (screen shows the old one)', () => {
    render(revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']));
    expect(getButtonByText('結案')?.disabled).toBeFalse();

    const newer = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    const newer$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newer$);
    newer.rereviewContext!.basedOnVersion = { submissionId: 'SUB-REREVIEW-003', version: 3, status: 'SUBMITTED' };
    component.applyCaseDetail(newer);
    fixture.detectChanges();

    expect(getButtonByText('結案')?.disabled).toBeTrue();
    getButtonByText('結案')?.click();
    expect(casesService.closeRereview).not.toHaveBeenCalled();

    newer$.next(buildSubmissionDetail({ submissionId: 'SUB-REREVIEW-003', revisionNo: 3, status: 'SUBMITTED' }));
    fixture.detectChanges();
    expect(getButtonByText('結案')?.disabled).toBeFalse();
  });

  it('locks the form while the close command and the following reload are in flight', async () => {
    render(revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']));
    const close$ = new Subject<{ caseId: string; caseStatus: string; submissionId: string }>();
    const reload$ = new Subject<CaseDetailViewModel>();
    casesService.closeRereview.and.returnValue(close$);
    casesService.getCaseDetail.and.returnValue(reload$);

    const answer = () => fixture.nativeElement.querySelector('[data-testid="rereview-answer-input-0"]') as HTMLInputElement;
    const comment = () => fixture.nativeElement.querySelector('[data-testid="rereview-comment-input"]') as HTMLTextAreaElement;
    expect(answer().disabled).toBeFalse();
    expect(comment().disabled).toBeFalse();

    getButtonByText('結案')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(answer().disabled).withContext('結案請求進行中').toBeTrue();
    expect(comment().disabled).withContext('結案請求進行中').toBeTrue();
    expect(getButtonByText('提交複審修正')?.disabled).withContext('結案請求進行中').toBeTrue();

    close$.next({ caseId: 'CASE-013', caseStatus: 'CLOSED', submissionId: 'SUB-REREVIEW-002' });
    close$.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(answer().disabled).withContext('重新載入進行中').toBeTrue();
    expect(comment().disabled).withContext('重新載入進行中').toBeTrue();
    expect(getButtonByText('結案')?.disabled).withContext('重新載入進行中').toBeTrue();
    expect(getButtonByText('提交複審修正')?.disabled).withContext('重新載入進行中').toBeTrue();

    reload$.next(revisionDetail(['REVISE_REREVIEW']));
    reload$.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(answer().disabled).withContext('重新載入完成').toBeFalse();
    expect(comment().disabled).withContext('重新載入完成').toBeFalse();
  });

  it('locks the draft total score input while the save-draft command and the following reload are in flight', async () => {
    const draftDetail = () => buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
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
      }
    });
    render(draftDetail());
    const save$ = new Subject<{ submissionId: string; status: string; stage: string; totalScore: number }>();
    const reload$ = new Subject<CaseDetailViewModel>();
    casesService.saveRereviewDraft.and.returnValue(save$);
    casesService.getCaseDetail.and.returnValue(reload$);

    const score = () => fixture.nativeElement.querySelector('[data-testid="rereview-total-score-input"]') as HTMLInputElement;
    await fixture.whenStable();
    expect(score().disabled).toBeFalse();

    setInputValue('[data-testid="rereview-total-score-input"]', '91');
    getButtonByText('保存複審草稿')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(score().disabled).withContext('保存請求進行中').toBeTrue();

    save$.next({ submissionId: 'SUB-REREVIEW-001', status: 'DRAFT', stage: 'REREVIEW', totalScore: 91 });
    save$.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(score().disabled).withContext('重新載入進行中').toBeTrue();

    reload$.next(draftDetail());
    reload$.complete();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(score().disabled).withContext('重新載入完成').toBeFalse();
  });

  // ---- PR #58 審查：案件詳情重新載入完成後，提交明細仍在載入時，表單與命令按鈕須持續鎖定 ----

  it('keeps the form and revise button locked until the new submission detail has loaded after a revise', () => {
    render(revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']));
    casesService.reviseRereview.and.returnValue(of({ oldSubmissionId: 'SUB-REREVIEW-002', newSubmissionId: 'SUB-REREVIEW-003' }));

    const newer = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    newer.rereviewContext!.basedOnVersion = { submissionId: 'SUB-REREVIEW-003', version: 3, status: 'SUBMITTED' };
    const newerSubmission$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newerSubmission$);
    casesService.getCaseDetail.and.returnValue(of(newer));

    const answer = () => fixture.nativeElement.querySelector('[data-testid="rereview-answer-input-0"]') as HTMLInputElement;
    const comment = () => fixture.nativeElement.querySelector('[data-testid="rereview-comment-input"]') as HTMLTextAreaElement;

    getButtonByText('提交複審修正')?.click();
    fixture.detectChanges();

    expect(component.reloadPending).withContext('案件詳情已載入完成').toBeFalse();
    expect(component.formLocked).withContext('提交明細仍在載入').toBeTrue();
    expect(getButtonByText('提交複審修正')?.disabled).withContext('提交明細仍在載入').toBeTrue();
    expect(getButtonByText('結案')?.disabled).withContext('提交明細仍在載入').toBeTrue();
    getButtonByText('提交複審修正')?.click();
    expect(casesService.reviseRereview).toHaveBeenCalledTimes(1);

    newerSubmission$.next(buildSubmissionDetail({
      submissionId: 'SUB-REREVIEW-003', revisionNo: 3, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-3', answerValue: 'Y' }], comment: 'newer'
    }));
    newerSubmission$.complete();
    fixture.detectChanges();

    expect(component.formLocked).withContext('提交明細載入完成').toBeFalse();
    expect(getButtonByText('提交複審修正')?.disabled).withContext('提交明細載入完成').toBeFalse();
    expect(component.formComment).toBe('newer');
  });

  it('unlocks the form when the submission detail fails to load, while keeping close disabled', () => {
    render(revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']));
    expect(component.formLocked).toBeFalse();

    const newer = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    newer.rereviewContext!.basedOnVersion = { submissionId: 'SUB-REREVIEW-003', version: 3, status: 'SUBMITTED' };
    casesService.getSubmissionDetail.and.returnValue(throwError(() => ({ status: 500 })));
    component.applyCaseDetail(newer);
    fixture.detectChanges();

    expect(component.formLocked).toBeFalse();
    expect(getButtonByText('結案')?.disabled).toBeTrue();
  });

  it('does not unlock when an older submission load resolves while a newer one is still pending', () => {
    const older$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    const detail = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    casesService.getSubmissionDetail.and.returnValue(older$);
    render(detail);
    expect(component.formLocked).toBeTrue();

    const newer = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    newer.rereviewContext!.basedOnVersion = { submissionId: 'SUB-REREVIEW-003', version: 3, status: 'SUBMITTED' };
    const newer$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newer$);
    component.applyCaseDetail(newer);
    fixture.detectChanges();

    older$.next(revisionBaseSubmission());
    older$.complete();
    fixture.detectChanges();
    expect(component.formLocked).withContext('較舊請求已回應，較新請求仍在載入').toBeTrue();
    expect(getButtonByText('提交複審修正')?.disabled).withContext('較新請求仍在載入').toBeTrue();

    newer$.next(buildSubmissionDetail({ submissionId: 'SUB-REREVIEW-003', revisionNo: 3, status: 'SUBMITTED' }));
    fixture.detectChanges();
    expect(component.formLocked).toBeFalse();
    expect(getButtonByText('結案')?.disabled).toBeFalse();
  });

  it('keeps save-draft disabled until the draft submission detail has loaded (avoids saving an empty form)', () => {
    const draft$ = new Subject<ReturnType<typeof buildSubmissionDetail>>();
    casesService.getSubmissionDetail.and.returnValue(draft$);
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: false,
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
      }
    }));

    expect(getButtonByText('保存複審草稿')?.disabled).toBeTrue();
    expect(getButtonByText('提交複審')?.disabled).toBeTrue();
    getButtonByText('保存複審草稿')?.click();
    expect(casesService.saveRereviewDraft).not.toHaveBeenCalled();

    draft$.next(buildSubmissionDetail());
    fixture.detectChanges();
    expect(getButtonByText('保存複審草稿')?.disabled).toBeFalse();
    expect(getButtonByText('提交複審')?.disabled).toBeFalse();
  });

  // ---- 被取代的提交明細載入：較晚回應不得覆蓋表單或留下鎖定 ----

  it('releases the lock when a pending submission load is superseded by a page with no form source, and ignores its late response', () => {
    const detail = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    const stale$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(stale$);
    render(detail);
    expect(component.formLocked).withContext('A 載入中').toBeTrue();

    component.applyCaseDetail(buildDetail());
    fixture.detectChanges();
    expect(component.mode).toBe('NEW');
    expect(component.submissionLoadPending).withContext('改為無表單來源的頁面').toBeFalse();
    expect(component.formLocked).withContext('改為無表單來源的頁面').toBeFalse();
    expect(getButtonByText('開始複審')?.disabled).withContext('依 allowedActions').toBeFalse();

    stale$.next(revisionBaseSubmission());
    stale$.complete();
    fixture.detectChanges();
    expect(component.formAnswers).withContext('A 的較晚回應').toEqual([]);
    expect(component.formComment).withContext('A 的較晚回應').toBe('');
    expect(component.formSourceSubmissionId).withContext('A 的較晚回應').toBeUndefined();
  });

  it('releases the lock when the page returns to an already loaded submission while a newer load is pending, and ignores the newer late response', () => {
    render(revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']));
    expect(component.formSourceSubmissionId).toBe('SUB-REREVIEW-002');

    const newer = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
    newer.rereviewContext!.basedOnVersion = { submissionId: 'SUB-REREVIEW-003', version: 3, status: 'SUBMITTED' };
    const newer$ = new Subject<ReturnType<typeof revisionBaseSubmission>>();
    casesService.getSubmissionDetail.and.returnValue(newer$);
    component.applyCaseDetail(newer);
    fixture.detectChanges();
    expect(component.formLocked).withContext('B 載入中').toBeTrue();

    component.applyCaseDetail(revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']));
    fixture.detectChanges();
    expect(component.submissionLoadPending).withContext('回到已載入的 A').toBeFalse();
    expect(component.formLocked).withContext('回到已載入的 A').toBeFalse();
    expect(getButtonByText('提交複審修正')?.disabled).withContext('回到已載入的 A').toBeFalse();
    expect(getButtonByText('結案')?.disabled).withContext('回到已載入的 A').toBeFalse();

    newer$.next(buildSubmissionDetail({
      submissionId: 'SUB-REREVIEW-003', revisionNo: 3, status: 'SUBMITTED',
      answers: [{ itemId: 'ITEM-3', answerValue: 'Y' }], comment: 'stale newer'
    }));
    newer$.complete();
    fixture.detectChanges();
    expect(component.formSourceSubmissionId).withContext('B 的較晚回應').toBe('SUB-REREVIEW-002');
    expect(component.formComment).withContext('B 的較晚回應').toBe('revision base');
    expect(component.formAnswers).withContext('B 的較晚回應').toEqual([{ itemId: 'ITEM-2', answerValue: 'N' }]);
  });

  it('ignores a late error from a superseded submission load (no feedback, no form reset)', () => {
    const detail = revisionDetail(['REVISE_REREVIEW', 'CLOSE_REREVIEW']);
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
    expect(getButtonByText('開始複審')?.disabled).toBeFalse();
  });
});
