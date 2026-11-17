import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize, Observable, switchMap } from 'rxjs';

import { SubmissionRef } from '../components/review-version-summary-region/review-version-summary-region.component';
import {
  CaseDetailViewModel,
  CasesService,
  CommandResultViewModel,
  InitialReviewBootstrapViewModel,
  InitialReviewDraftPayload,
  SubmissionDetailViewModel
} from '../services/cases.service';

export type InitialReviewMode = 'NEW' | 'EDIT_DRAFT' | 'REVISION' | 'READONLY_BLOCKED';

export interface InitialReviewContext {
  caseId: string;
  caseTitle: string;
  status: string;
  stageLabel: string;
  blockedReason: string | null;
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  basedOnVersion?: SubmissionRef;
}

interface InitialReviewAnswerState {
  itemId: string;
  answerValue: string;
}

@Component({
  selector: 'app-initial-review-page',
  templateUrl: './initial-review-page.component.html',
  styleUrls: ['./initial-review-page.component.scss']
})
export class InitialReviewPageComponent implements OnInit {
  caseId = '';
  context: InitialReviewContext = this.createDefaultContext('');
  mode: InitialReviewMode = 'NEW';
  modeLabel = 'NEW';
  bannerMessage = '';
  commandPending = false;
  /** 命令成功或失敗後重新載入頁面，載入完成前視同命令仍在進行 */
  reloadPending = false;
  /** 表單來源提交明細載入中；載入完成前表單內容仍是舊版，視同重新載入尚未完成 */
  submissionLoadPending = false;
  /** 每次發出提交明細載入就遞增；只有最新一次的回應可以寫入表單或解除鎖定 */
  private submissionLoadSeq = 0;
  commandFeedbackMessage = '';
  commandFeedbackTone: 'info' | 'blocked' = 'info';
  formAnswers: InitialReviewAnswerState[] = [];
  formComment = '';
  formSourceSubmissionId?: string;
  formStateLoaded = false;
  formStateDirty = false;
  private bootstrap?: InitialReviewBootstrapViewModel;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly casesService: CasesService
  ) {}

  ngOnInit(): void {
    this.caseId = this.route.snapshot.paramMap.get('caseId') ?? 'CASE-WAVE5';
    this.casesService.getCaseDetail(this.caseId).subscribe({
      next: detail => this.applyCaseDetail(detail),
      error: () => {
        this.applyContext({
          ...this.createDefaultContext(this.caseId),
          status: 'CANCELLED',
          stageLabel: '載入失敗',
          blockedReason: 'CASE_DETAIL_LOAD_FAILED'
        });
      }
    });
  }

  applyCaseDetail(detail: CaseDetailViewModel): void {
    const bootstrap = detail.initialReviewContext;
    this.bootstrap = bootstrap;

    this.applyContext({
      caseId: detail.caseId,
      caseTitle: '初審作業',
      status: detail.status,
      stageLabel: this.resolveStageLabel(detail.status),
      blockedReason: bootstrap?.blockedReason ?? null,
      hasDraft: bootstrap?.hasDraft ?? false,
      hasSubmittedBase: bootstrap?.hasSubmittedBase ?? false,
      basedOnVersion: bootstrap?.basedOnVersion
    }, bootstrap);

    this.syncFormState();
  }

  applyContext(context: InitialReviewContext, bootstrap?: InitialReviewBootstrapViewModel): void {
    this.context = context;
    this.caseId = context.caseId;
    this.resolveMode(context, bootstrap);
  }

  resolveMode(context: InitialReviewContext, bootstrap?: InitialReviewBootstrapViewModel): void {
    const backendMode = bootstrap?.pageMode;

    if ((!backendMode && context.blockedReason) || ['CLOSED', 'CANCELLED'].includes(context.status)) {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerMessage = '案件目前不可執行初審作業。';
      return;
    }

    if (backendMode === 'READONLY_BLOCKED') {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerMessage = context.blockedReason === 'INITIAL_REVIEW_FIXED'
        ? '初審已固定，不可再修改。'
        : '案件目前不可執行初審作業。';
      return;
    }

    if (backendMode === 'REVISION') {
      this.mode = 'REVISION';
      this.modeLabel = 'REVISION';
      this.bannerMessage = '';
      return;
    }

    if (backendMode === 'EDIT_DRAFT') {
      this.mode = 'EDIT_DRAFT';
      this.modeLabel = 'EDIT_DRAFT';
      this.bannerMessage = '';
      return;
    }

    this.mode = 'NEW';
    this.modeLabel = 'NEW';
    this.bannerMessage = '';
  }

  get showBlockedBanner(): boolean {
    return this.mode === 'READONLY_BLOCKED';
  }

  get showVersionSummary(): boolean {
    return this.mode === 'REVISION';
  }

  get showCommandFeedback(): boolean {
    return this.commandFeedbackMessage.length > 0;
  }

  /** 唯讀、命令進行中或重新載入中時鎖定表單，避免輸入在重新載入後遺失 */
  get formLocked(): boolean {
    return this.mode === 'READONLY_BLOCKED' || this.commandPending || this.reloadPending || this.submissionLoadPending;
  }

  get showCommandBar(): boolean {
    return this.mode !== 'READONLY_BLOCKED';
  }

  get commandPrimaryLabel(): string {
    switch (this.mode) {
      case 'NEW':
        return '開始初審';
      case 'EDIT_DRAFT':
        return '提交初審';
      case 'REVISION':
        return '提交初審修正';
      default:
        return '提交初審';
    }
  }

  get commandSecondaryLabel(): string | undefined {
    if (this.mode === 'EDIT_DRAFT') {
      return '保存草稿';
    }

    // D-054 第 9 點：已提交、未固定時，由提交者另行固定；是否允許以後端的 allowedActions 為準
    return this.mode === 'REVISION' ? '固定初審' : undefined;
  }

  get commandPrimaryDisabled(): boolean {
    if (this.commandPending || this.reloadPending || this.submissionLoadPending || this.isBlockedByBackendTruth()) {
      return true;
    }

    switch (this.mode) {
      case 'NEW':
        return !this.hasAllowedAction('START_INITIAL_REVIEW');
      case 'EDIT_DRAFT':
        return !this.hasAllowedAction('SUBMIT_INITIAL_REVIEW');
      case 'REVISION':
        return !this.hasAllowedAction('REVISE_INITIAL_REVIEW') || !this.resolveInitialRevisionBaseSubmissionId();
      default:
        return true;
    }
  }

  get commandSecondaryDisabled(): boolean {
    if (this.commandPending || this.reloadPending || this.submissionLoadPending || this.isBlockedByBackendTruth()) {
      return true;
    }

    if (this.mode === 'REVISION') {
      // 表單有尚未提交的修改時不能固定：固定的是目前有效的版本，修改會在重新載入後遺失
      // 畫面上必須已載入要固定的那一版：載入中、載入失敗或版本不符時都不能固定
      const target = this.resolveInitialRevisionBaseSubmissionId();
      return this.formStateDirty
        || !this.hasAllowedAction('FIX_INITIAL_REVIEW')
        || !target
        || !this.formStateLoaded
        || this.formSourceSubmissionId !== target;
    }

    if (this.mode !== 'EDIT_DRAFT') {
      return true;
    }

    return !this.hasAllowedAction('SAVE_INITIAL_REVIEW_DRAFT') || !this.resolveInitialEditableSubmissionId();
  }

  onPrimaryAction(): void {
    if (this.commandPrimaryDisabled) {
      return;
    }

    if (this.mode === 'NEW') {
      this.executeCommand(
        this.casesService.startInitialReview(this.caseId),
        () => '已開始初審，系統已建立最小草稿。'
      );
      return;
    }

    if (this.mode === 'EDIT_DRAFT') {
      const editableSubmissionId = this.resolveInitialEditableSubmissionId();
      if (!editableSubmissionId) {
        this.setCommandFeedback('無法取得可提交的初審草稿。', 'blocked');
        return;
      }

      this.executeCommand(
        this.casesService.saveInitialReviewDraft(this.caseId, this.buildInitialPayloadFromState()).pipe(
          switchMap(() => this.casesService.submitInitialReview(this.caseId))
        ),
        // D-054 第 3 點：提交後一律停在 INITIAL_REVIEW_COMPLETED，固定時才判斷是否進複審
        () => '初審已提交，尚未固定。確認結果無誤後，請按「固定初審」。'
      );
      return;
    }

    if (this.mode === 'REVISION') {
      const baseSubmissionId = this.resolveInitialRevisionBaseSubmissionId();
      if (!baseSubmissionId) {
        this.setCommandFeedback('無法取得初審修正基底版本。', 'blocked');
        return;
      }

      this.executeCommand(
        this.casesService.reviseInitialReview(this.caseId, baseSubmissionId, this.buildInitialPayloadFromState()),
        () => '初審修正已提交。'
      );
    }
  }

  onSecondaryAction(): void {
    if (this.commandSecondaryDisabled) {
      return;
    }

    if (this.mode === 'REVISION') {
      const submissionId = this.resolveInitialRevisionBaseSubmissionId();
      if (!submissionId) {
        this.setCommandFeedback('無法取得要固定的初審版本。', 'blocked');
        return;
      }

      this.executeCommand(
        this.casesService.fixInitialReview(this.caseId, submissionId),
        result => result.caseStatus === 'PENDING_REREVIEW'
          ? '初審已固定，案件已送往複審。'
          : '初審已固定，案件不需複審，由管理者決定後續處理。',
        // D-054 第 7 點：重複固定等失敗時重新整理畫面，以後端狀態為準
        '固定初審失敗，已重新整理頁面。'
      );
      return;
    }

    const editableSubmissionId = this.resolveInitialEditableSubmissionId();
    if (!editableSubmissionId) {
      this.setCommandFeedback('無法取得可保存的初審草稿。', 'blocked');
      return;
    }

    this.executeCommand(
      this.casesService.saveInitialReviewDraft(this.caseId, this.buildInitialPayloadFromState()),
      result => result.noChanges ? '草稿已是最新，沒有需要保存的變更。' : '初審草稿已保存。'
    );
  }

  onAnswerValueChange(index: number, value: string): void {
    if (!this.formAnswers[index]) {
      return;
    }

    this.formAnswers[index] = {
      ...this.formAnswers[index],
      answerValue: value
    };
    this.formStateDirty = true;
  }

  onCommentChange(value: string): void {
    this.formComment = value;
    this.formStateDirty = true;
  }

  private createDefaultContext(caseId: string): InitialReviewContext {
    return {
      caseId,
      caseTitle: '初審作業',
      status: 'PENDING_INITIAL_REVIEW',
      stageLabel: '待初審',
      blockedReason: null,
      hasDraft: false,
      hasSubmittedBase: false
    };
  }

  private resolveStageLabel(status: string): string {
    switch (status) {
      case 'INITIAL_REVIEW_IN_PROGRESS':
        return '初審進行中';
      case 'INITIAL_REVIEW_COMPLETED':
        return '初審完成';
      case 'PENDING_REREVIEW':
        return '待複審';
      case 'REREVIEW_IN_PROGRESS':
        return '複審進行中';
      case 'CLOSED':
        return '已結案';
      case 'CANCELLED':
        return '已取消';
      default:
        return '待初審';
    }
  }

  private buildInitialPayloadFromState(): InitialReviewDraftPayload {
    return {
      answers: this.formAnswers.map(answer => ({
        itemId: answer.itemId,
        answerValue: answer.answerValue
      })),
      comment: this.formComment || null
    };
  }

  private syncFormState(): void {
    const sourceSubmissionId = this.resolveFormSourceSubmissionId();

    if (!sourceSubmissionId) {
      this.clearSubmissionLoad();
      this.resetFormState();
      return;
    }

    if (this.formSourceSubmissionId === sourceSubmissionId && this.formStateLoaded) {
      this.clearSubmissionLoad();
      return;
    }

    this.submissionLoadPending = true;
    const seq = ++this.submissionLoadSeq;
    this.casesService.getSubmissionDetail(sourceSubmissionId).pipe(finalize(() => {
      this.endSubmissionLoad(seq);
    })).subscribe({
      next: detail => {
        if (seq !== this.submissionLoadSeq) {
          return; // 已被較新的載入或頁面狀態取代，忽略較晚的回應
        }
        this.endSubmissionLoad(seq);
        this.hydrateFormState(detail);
      },
      error: () => {
        if (seq !== this.submissionLoadSeq) {
          return; // 已被取代的載入失敗時，不重設表單也不顯示訊息
        }
        this.endSubmissionLoad(seq);
        this.resetFormState(sourceSubmissionId);
        this.setCommandFeedback('初審表單狀態載入失敗，僅保留最小骨架。', 'blocked');
      }
    });
  }

  /**
   * 只由最後一次發出的載入解除鎖定，避免較舊的請求提早解鎖。
   * 以請求序號而非提交 id 判斷：同一筆提交重複載入時，較舊請求的回應也會被視為過期。
   */
  private endSubmissionLoad(seq: number): void {
    if (seq === this.submissionLoadSeq) {
      this.submissionLoadPending = false;
    }
  }

  /** 不再需要等待任何提交明細時解除鎖定；遞增序號，讓仍在途中的回應一律被視為過期而忽略 */
  private clearSubmissionLoad(): void {
    this.submissionLoadSeq++;
    this.submissionLoadPending = false;
  }

  private hydrateFormState(detail: SubmissionDetailViewModel): void {
    this.formSourceSubmissionId = detail.submissionId;
    this.formAnswers = detail.answers.map(answer => ({
      itemId: answer.itemId,
      answerValue: answer.answerValue
    }));
    this.formComment = detail.comment ?? '';
    this.formStateLoaded = true;
    this.formStateDirty = false;
  }

  private resetFormState(sourceSubmissionId?: string): void {
    this.formSourceSubmissionId = sourceSubmissionId;
    this.formAnswers = [];
    this.formComment = '';
    this.formStateLoaded = !sourceSubmissionId;
    this.formStateDirty = false;
  }

  private resolveFormSourceSubmissionId(): string | undefined {
    if (this.mode === 'EDIT_DRAFT') {
      return this.resolveInitialEditableSubmissionId();
    }

    if (this.mode === 'REVISION') {
      return this.resolveInitialRevisionBaseSubmissionId();
    }

    return undefined;
  }

  private resolveInitialEditableSubmissionId(): string | undefined {
    return this.bootstrap?.editableSubmission?.submissionId ?? this.bootstrap?.currentSubmission?.submissionId;
  }

  private resolveInitialRevisionBaseSubmissionId(): string | undefined {
    return this.bootstrap?.basedOnVersion?.submissionId;
  }

  private hasAllowedAction(action: string): boolean {
    return this.bootstrap?.allowedActions.includes(action) ?? false;
  }

  private isBlockedByBackendTruth(): boolean {
    return Boolean(this.bootstrap?.blocked || this.bootstrap?.blockedReason || this.mode === 'READONLY_BLOCKED');
  }

  private executeCommand(
    command$: Observable<CommandResultViewModel>,
    successMessage: (result: CommandResultViewModel) => string,
    reloadOnErrorMessage?: string
  ): void {
    this.commandPending = true;
    this.commandFeedbackMessage = '';

    command$.pipe(finalize(() => {
      this.commandPending = false;
    })).subscribe({
      next: result => {
        this.formStateDirty = false;
        this.setCommandFeedback(successMessage(result), 'info');
        this.reloadCaseDetail();
      },
      error: () => {
        if (reloadOnErrorMessage) {
          this.setCommandFeedback(reloadOnErrorMessage, 'blocked');
          this.reloadCaseDetail();
          return;
        }
        this.setCommandFeedback('初審 command 執行失敗，limited wiring 僅提供最小錯誤提示。', 'blocked');
      }
    });
  }

  private reloadCaseDetail(): void {
    this.reloadPending = true;
    this.casesService.getCaseDetail(this.caseId).pipe(finalize(() => {
      this.reloadPending = false;
    })).subscribe({
      next: detail => this.applyCaseDetail(detail),
      error: () => this.setCommandFeedback('重新整理初審頁面 bootstrap 失敗。', 'blocked')
    });
  }

  private setCommandFeedback(message: string, tone: 'info' | 'blocked'): void {
    this.commandFeedbackMessage = message;
    this.commandFeedbackTone = tone;
  }
}
