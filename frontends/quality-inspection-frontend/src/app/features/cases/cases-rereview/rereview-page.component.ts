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
  | 'CLOSED_REVISION_EXCEPTION'
  | 'READONLY_BLOCKED';

export interface RereviewContext {
  caseId: string;
  caseTitle: string;
  status: string;
  stageLabel: string;
  blockedReason: string | null;
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  isClosedRevisionException: boolean;
  basedOnVersion?: SubmissionRef;
  initialSubmissionId: string;
  initialReviewerName: string;
  initialTotalScore: string;
  finalDecisionSummary: string;
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
  bannerTone: 'blocked' | 'exception-amber' = 'blocked';
  bannerMessage = '';
  commandPending = false;
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
      isClosedRevisionException: bootstrap?.closedRevisionException ?? false,
      basedOnVersion: bootstrap?.basedOnVersion,
      initialSubmissionId: bootstrap?.initialSubmissionId ?? '',
      initialReviewerName: bootstrap?.initialReviewerName || bootstrap?.initialReviewerId || '未提供',
      initialTotalScore: bootstrap?.initialTotalScore != null ? String(bootstrap.initialTotalScore) : '未提供',
      finalDecisionSummary: this.buildFinalDecisionSummary(bootstrap)
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

    if ((!backendMode && context.blockedReason) || ['CLOSED', 'CANCELLED'].includes(context.status) && !context.isClosedRevisionException) {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerTone = 'blocked';
      this.bannerMessage = '案件目前不可執行複審作業。';
      return;
    }

    if (backendMode === 'REVISION_EXCEPTION_CLOSED') {
      this.mode = 'CLOSED_REVISION_EXCEPTION';
      this.modeLabel = 'CLOSED_REVISION_EXCEPTION';
      this.bannerTone = 'exception-amber';
      this.bannerMessage = '此案件已結案，但目前以例外修正骨架呈現。';
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

  get showExceptionBanner(): boolean {
    return this.mode === 'CLOSED_REVISION_EXCEPTION';
  }

  get showBlockedBanner(): boolean {
    return this.mode === 'READONLY_BLOCKED';
  }

  get showVersionSummary(): boolean {
    return this.mode === 'REVISION' || this.mode === 'CLOSED_REVISION_EXCEPTION';
  }

  get showCommandFeedback(): boolean {
    return this.commandFeedbackMessage.length > 0;
  }

  get showCommandBar(): boolean {
    return this.mode !== 'READONLY_BLOCKED';
  }

  get commandPrimaryLabel(): string {
    if (this.mode === 'NEW') {
      return '開始複審';
    }

    if (this.mode === 'CLOSED_REVISION_EXCEPTION') {
      return '提交複審修正';
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
    return this.mode === 'EDIT_DRAFT' ? '保存複審草稿' : undefined;
  }

  get commandPrimaryDisabled(): boolean {
    if (this.commandPending || this.isBlockedByBackendTruth()) {
      return true;
    }

    switch (this.mode) {
      case 'NEW':
        return !this.hasAllowedAction('START_REREVIEW');
      case 'EDIT_DRAFT':
        return !this.hasAllowedAction('SUBMIT_REREVIEW') || !this.resolveRereviewEditableSubmissionId();
      case 'REVISION':
      case 'CLOSED_REVISION_EXCEPTION':
        return !this.hasAllowedAction('REVISE_REREVIEW') || !this.resolveRereviewRevisionBaseSubmissionId();
      default:
        return true;
    }
  }

  get commandSecondaryDisabled(): boolean {
    if (this.commandPending || this.isBlockedByBackendTruth()) {
      return true;
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
        result => result.finalScore != null ? `複審已提交，最終分數 ${result.finalScore}。` : '複審已提交。'
      );
      return;
    }

    if (this.mode === 'REVISION' || this.mode === 'CLOSED_REVISION_EXCEPTION') {
      const submissionId = this.resolveRereviewRevisionBaseSubmissionId();
      if (!submissionId) {
        this.setCommandFeedback('無法取得複審修正基底版本。', 'blocked');
        return;
      }

      this.executeCommand(
        this.casesService.reviseRereview(this.caseId, submissionId, this.buildRereviewRevisionPayloadFromState()),
        () => this.mode === 'CLOSED_REVISION_EXCEPTION' ? '已提交已結案例外複審修正。' : '複審修正已提交。'
      );
    }
  }

  onSecondaryAction(): void {
    if (this.commandSecondaryDisabled) {
      return;
    }

    const submissionId = this.resolveRereviewEditableSubmissionId();
    if (!submissionId) {
      this.setCommandFeedback('無法取得可保存的複審草稿。', 'blocked');
      return;
    }

    this.executeCommand(
      this.casesService.saveRereviewDraft(this.caseId, this.buildRereviewDraftPayloadFromState(submissionId)),
      () => '複審草稿已保存。'
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
      isClosedRevisionException: false,
      initialSubmissionId: 'SUB-INITIAL-001',
      initialReviewerName: '原初審醫師',
      initialTotalScore: '82',
      finalDecisionSummary: '最終結論 placeholder'
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

  private buildFinalDecisionSummary(bootstrap?: RereviewBootstrapViewModel): string {
    if (!bootstrap) {
      return '最終結論 placeholder';
    }

    const parts = [bootstrap.finalConclusion, bootstrap.finalScore != null ? `最終分數 ${bootstrap.finalScore}` : null]
      .filter((value): value is string => Boolean(value));

    return parts.join(' / ') || '最終結論未提供';
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

    this.casesService.getSubmissionDetail(sourceSubmissionId).subscribe({
      next: detail => this.hydrateFormState(detail),
      error: () => {
        this.resetFormState(sourceSubmissionId);
        this.setCommandFeedback('複審表單狀態載入失敗，僅保留最小骨架。', 'blocked');
      }
    });
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

    if (this.mode === 'REVISION' || this.mode === 'CLOSED_REVISION_EXCEPTION') {
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
      error: () => {
        this.setCommandFeedback('複審 command 執行失敗，limited wiring 僅提供最小錯誤提示。', 'blocked');
      }
    });
  }

  private reloadCaseDetail(): void {
    this.casesService.getCaseDetail(this.caseId).subscribe({
      next: detail => this.applyCaseDetail(detail),
      error: () => this.setCommandFeedback('重新整理複審頁面 bootstrap 失敗。', 'blocked')
    });
  }

  private setCommandFeedback(message: string, tone: 'info' | 'blocked'): void {
    this.commandFeedbackMessage = message;
    this.commandFeedbackTone = tone;
  }
}
