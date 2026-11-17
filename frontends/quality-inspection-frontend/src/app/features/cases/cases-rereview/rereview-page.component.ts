import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize, Observable, switchMap } from 'rxjs';

import { SubmissionRef } from '../components/review-version-summary-region/review-version-summary-region.component';
import {
  CaseDetailViewModel,
  CasesService,
  CommandResultViewModel,
  RereviewBootstrapViewModel,
  RereviewDraftPayload,
  RereviewSubmitPayload,
  SubmissionDetailViewModel
} from '../services/cases.service';

export type RereviewMode =
  | 'NEW'
  | 'EDIT_DRAFT'
  | 'REVISION'
  | 'READONLY_BLOCKED';

export interface RereviewContext {
  caseId: string;
  caseTitle: string;
  status: string;
  stageLabel: string;
  blockedReason: string | null;
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  basedOnVersion?: SubmissionRef;
  initialSubmissionId: string;
  initialReviewerName: string;
}

interface RereviewAnswerState {
  itemId: string;
  answerValue: string;
}

@Component({
  selector: 'app-rereview-page',
  templateUrl: './rereview-page.component.html',
  styleUrls: ['./rereview-page.component.scss']
})
export class RereviewPageComponent implements OnInit {
  caseId = '';
  context: RereviewContext = this.createDefaultContext('');
  mode: RereviewMode = 'NEW';
  modeLabel = 'NEW';
  bannerTone: 'blocked' = 'blocked';
  bannerMessage = '';
  commandPending = false;
  /** 命令成功或失敗後重新載入頁面，載入完成前視同命令仍在進行 */
  reloadPending = false;
  /** 表單來源提交明細載入中；載入完成前表單內容仍是舊版，視同重新載入尚未完成 */
  submissionLoadPending = false;
  private loadingSubmissionId?: string;
  commandFeedbackMessage = '';
  commandFeedbackTone: 'info' | 'blocked' = 'info';
  formAnswers: RereviewAnswerState[] = [];
  formComment = '';
  draftTotalScoreInput: string | number = '';
  formSourceSubmissionId?: string;
  formStateLoaded = false;
  formStateDirty = false;
  private bootstrap?: RereviewBootstrapViewModel;

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
    const bootstrap = detail.rereviewContext;
    this.bootstrap = bootstrap;

    this.applyContext({
      caseId: detail.caseId,
      caseTitle: '複審作業',
      status: detail.status,
      stageLabel: this.resolveStageLabel(detail.status),
      blockedReason: bootstrap?.blockedReason ?? null,
      hasDraft: bootstrap?.hasDraft ?? false,
      hasSubmittedBase: bootstrap?.hasSubmittedBase ?? false,
      basedOnVersion: bootstrap?.basedOnVersion,
      initialSubmissionId: bootstrap?.initialSubmissionId ?? '',
      initialReviewerName: bootstrap?.initialReviewerName || bootstrap?.initialReviewerId || '未提供'
    }, bootstrap);

    this.syncFormState();
  }

  applyContext(context: RereviewContext, bootstrap?: RereviewBootstrapViewModel): void {
    this.context = context;
    this.caseId = context.caseId;
    this.resolveMode(context, bootstrap);
  }

  resolveMode(context: RereviewContext, bootstrap?: RereviewBootstrapViewModel): void {
    const backendMode = bootstrap?.pageMode;

    // D-032：CLOSED 之後沒有例外修正，一律唯讀；CLOSED 代表複審結果已固定（D-053 第 3、8 點）
    if (context.status === 'CLOSED') {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerTone = 'blocked';
      this.bannerMessage = '案件已結案，複審結果已固定。';
      return;
    }

    if ((!backendMode && context.blockedReason) || context.status === 'CANCELLED') {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerTone = 'blocked';
      this.bannerMessage = '案件目前不可執行複審作業。';
      return;
    }

    if (backendMode === 'READONLY_BLOCKED') {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerTone = 'blocked';
      this.bannerMessage = '案件目前不可執行複審作業。';
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
    if (this.mode === 'NEW') {
      return '開始複審';
    }

    if (this.mode === 'REVISION') {
      return '提交複審修正';
    }

    if (this.mode === 'EDIT_DRAFT') {
      return '提交複審';
    }

    return '提交複審';
  }

  get commandSecondaryLabel(): string | undefined {
    if (this.mode === 'EDIT_DRAFT') {
      return '保存複審草稿';
    }

    // D-053 第 8 點：已提交、未結案時，由提交者另行結案；是否允許以後端的 allowedActions 為準
    return this.mode === 'REVISION' ? '結案' : undefined;
  }

  get commandPrimaryDisabled(): boolean {
    if (this.commandPending || this.reloadPending || this.submissionLoadPending || this.isBlockedByBackendTruth()) {
      return true;
    }

    switch (this.mode) {
      case 'NEW':
        return !this.hasAllowedAction('START_REREVIEW');
      case 'EDIT_DRAFT':
        return !this.hasAllowedAction('SUBMIT_REREVIEW') || !this.resolveRereviewEditableSubmissionId();
      case 'REVISION':
        return !this.hasAllowedAction('REVISE_REREVIEW') || !this.resolveRereviewRevisionBaseSubmissionId();
      default:
        return true;
    }
  }

  get commandSecondaryDisabled(): boolean {
    if (this.commandPending || this.reloadPending || this.submissionLoadPending || this.isBlockedByBackendTruth()) {
      return true;
    }

    if (this.mode === 'REVISION') {
      // 表單有尚未提交的修改時不能結案：結案會固定舊版，修改會在重新載入後遺失
      // 畫面上必須已載入要結案的那一版：載入中、載入失敗或版本不符時都不能結案
      const target = this.resolveRereviewRevisionBaseSubmissionId();
      return this.formStateDirty
        || !this.hasAllowedAction('CLOSE_REREVIEW')
        || !target
        || !this.formStateLoaded
        || this.formSourceSubmissionId !== target;
    }

    if (this.mode !== 'EDIT_DRAFT') {
      return true;
    }

    return !this.hasAllowedAction('SAVE_REREVIEW_DRAFT') || !this.resolveRereviewEditableSubmissionId();
  }

  onPrimaryAction(): void {
    if (this.commandPrimaryDisabled) {
      return;
    }

    if (this.mode === 'NEW') {
      this.executeCommand(
        this.casesService.startRereview(this.caseId),
        () => '已開始複審，系統已建立最小草稿。'
      );
      return;
    }

    if (this.mode === 'EDIT_DRAFT') {
      const submissionId = this.resolveRereviewEditableSubmissionId();
      if (!submissionId) {
        this.setCommandFeedback('無法取得可提交的複審草稿。', 'blocked');
        return;
      }

      this.executeCommand(
        this.casesService.submitRereview(this.caseId, this.buildRereviewSubmitPayloadFromState(submissionId)),
        () => '複審已提交，案件尚未結案。確認結果無誤後，請按「結案」。'
      );
      return;
    }

    if (this.mode === 'REVISION') {
      const submissionId = this.resolveRereviewRevisionBaseSubmissionId();
      if (!submissionId) {
        this.setCommandFeedback('無法取得複審修正基底版本。', 'blocked');
        return;
      }

      this.executeCommand(
        this.casesService.reviseRereview(this.caseId, submissionId, this.buildRereviewRevisionPayloadFromState()),
        () => '複審修正已提交。'
      );
    }
  }

  onSecondaryAction(): void {
    if (this.commandSecondaryDisabled) {
      return;
    }

    if (this.mode === 'REVISION') {
      const submissionId = this.resolveRereviewRevisionBaseSubmissionId();
      if (!submissionId) {
        this.setCommandFeedback('無法取得要結案的複審版本。', 'blocked');
        return;
      }

      this.executeCommand(
        this.casesService.closeRereview(this.caseId, submissionId),
        () => '案件已結案，複審結果已固定。'
      );
      return;
    }

    const submissionId = this.resolveRereviewEditableSubmissionId();
    if (!submissionId) {
      this.setCommandFeedback('無法取得可保存的複審草稿。', 'blocked');
      return;
    }

    this.executeCommand(
      this.casesService.saveRereviewDraft(this.caseId, this.buildRereviewDraftPayloadFromState(submissionId)),
      result => result.noChanges ? '草稿已是最新，沒有需要保存的變更。' : '複審草稿已保存。'
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

  onDraftTotalScoreChange(value: string | number): void {
    this.draftTotalScoreInput = value;
    this.formStateDirty = true;
  }

  private createDefaultContext(caseId: string): RereviewContext {
    return {
      caseId,
      caseTitle: '複審作業',
      status: 'PENDING_REREVIEW',
      stageLabel: '待複審',
      blockedReason: null,
      hasDraft: false,
      hasSubmittedBase: false,
      initialSubmissionId: 'SUB-INITIAL-001',
      initialReviewerName: '原初審醫師'
    };
  }

  private resolveStageLabel(status: string): string {
    switch (status) {
      case 'REREVIEW_IN_PROGRESS':
        return '複審進行中';
      case 'CLOSED':
        return '已結案';
      case 'CANCELLED':
        return '已取消';
      case 'INITIAL_REVIEW_COMPLETED':
        return '初審完成';
      default:
        return '待複審';
    }
  }

  private buildRereviewDraftPayloadFromState(submissionId: string): RereviewDraftPayload {
    return {
      submissionId,
      answers: this.formAnswers.map(answer => ({
        itemId: answer.itemId,
        answerValue: answer.answerValue
      })),
      comment: this.formComment || null,
      totalScore: this.parseDraftTotalScore()
    };
  }

  private buildRereviewSubmitPayloadFromState(submissionId: string): RereviewSubmitPayload {
    return {
      submissionId,
      answers: this.formAnswers.map(answer => ({
        itemId: answer.itemId,
        answerValue: answer.answerValue
      })),
      comment: this.formComment || null
    };
  }

  private buildRereviewRevisionPayloadFromState(): Pick<RereviewSubmitPayload, 'answers' | 'comment'> {
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
      this.resetFormState();
      return;
    }

    if (this.formSourceSubmissionId === sourceSubmissionId && this.formStateLoaded) {
      return;
    }

    this.submissionLoadPending = true;
    this.loadingSubmissionId = sourceSubmissionId;
    this.casesService.getSubmissionDetail(sourceSubmissionId).pipe(finalize(() => {
      this.endSubmissionLoad(sourceSubmissionId);
    })).subscribe({
      next: detail => {
        this.endSubmissionLoad(sourceSubmissionId);
        this.hydrateFormState(detail);
      },
      error: () => {
        this.endSubmissionLoad(sourceSubmissionId);
        this.resetFormState(sourceSubmissionId);
        this.setCommandFeedback('複審表單狀態載入失敗，僅保留最小骨架。', 'blocked');
      }
    });
  }

  /** 只由最後一次發出的載入解除鎖定，避免較舊的請求提早解鎖 */
  private endSubmissionLoad(sourceSubmissionId: string): void {
    if (this.loadingSubmissionId === sourceSubmissionId) {
      this.submissionLoadPending = false;
      this.loadingSubmissionId = undefined;
    }
  }

  private hydrateFormState(detail: SubmissionDetailViewModel): void {
    this.formSourceSubmissionId = detail.submissionId;
    this.formAnswers = detail.answers.map(answer => ({
      itemId: answer.itemId,
      answerValue: answer.answerValue
    }));
    this.formComment = detail.comment ?? '';
    this.draftTotalScoreInput = detail.totalScore != null ? String(detail.totalScore) : '';
    this.formStateLoaded = true;
    this.formStateDirty = false;
  }

  private resetFormState(sourceSubmissionId?: string): void {
    this.formSourceSubmissionId = sourceSubmissionId;
    this.formAnswers = [];
    this.formComment = '';
    this.draftTotalScoreInput = '';
    this.formStateLoaded = !sourceSubmissionId;
    this.formStateDirty = false;
  }

  private resolveFormSourceSubmissionId(): string | undefined {
    if (this.mode === 'EDIT_DRAFT') {
      return this.resolveRereviewEditableSubmissionId();
    }

    if (this.mode === 'REVISION') {
      return this.resolveRereviewRevisionBaseSubmissionId();
    }

    return undefined;
  }

  private parseDraftTotalScore(): number | null {
    const raw = String(this.draftTotalScoreInput ?? '').trim();

    if (!raw) {
      return null;
    }

    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private resolveRereviewEditableSubmissionId(): string | undefined {
    return this.bootstrap?.editableSubmission?.submissionId ?? this.bootstrap?.currentSubmission?.submissionId;
  }

  private resolveRereviewRevisionBaseSubmissionId(): string | undefined {
    return this.bootstrap?.basedOnVersion?.submissionId;
  }

  private hasAllowedAction(action: string): boolean {
    return this.bootstrap?.allowedActions.includes(action) ?? false;
  }

  private isBlockedByBackendTruth(): boolean {
    return Boolean(this.bootstrap?.blocked || this.bootstrap?.blockedReason || this.mode === 'READONLY_BLOCKED');
  }

  private executeCommand(command$: Observable<CommandResultViewModel>, successMessage: (result: CommandResultViewModel) => string): void {
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
      error: error => {
        // 案件已結案或已取消（例如另一個分頁已結案、或重送已成功的結案）：重新整理，讓畫面變成唯讀（D-053 第 6 點）
        if (error instanceof HttpErrorResponse && error.status === 409 && error.error?.error?.code === 'CASE_TERMINAL') {
          this.setCommandFeedback('案件已結案或已取消，畫面已重新整理。', 'blocked');
          this.reloadCaseDetail();
          return;
        }
        this.setCommandFeedback('複審 command 執行失敗，limited wiring 僅提供最小錯誤提示。', 'blocked');
      }
    });
  }

  private reloadCaseDetail(): void {
    this.reloadPending = true;
    this.casesService.getCaseDetail(this.caseId).pipe(finalize(() => {
      this.reloadPending = false;
    })).subscribe({
      next: detail => this.applyCaseDetail(detail),
      error: () => this.setCommandFeedback('重新整理複審頁面 bootstrap 失敗。', 'blocked')
    });
  }

  private setCommandFeedback(message: string, tone: 'info' | 'blocked'): void {
    this.commandFeedbackMessage = message;
    this.commandFeedbackTone = tone;
  }
}
