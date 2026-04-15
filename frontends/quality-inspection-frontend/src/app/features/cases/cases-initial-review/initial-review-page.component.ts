import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize, map, Observable, switchMap } from 'rxjs';

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
  commandFeedbackMessage = '';
  commandFeedbackTone: 'info' | 'blocked' = 'info';
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
      this.bannerMessage = '案件目前不可執行初審作業。';
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
    return this.mode === 'EDIT_DRAFT' ? '保存草稿' : undefined;
  }

  get commandPrimaryDisabled(): boolean {
    if (this.commandPending || this.isBlockedByBackendTruth()) {
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
    if (this.commandPending || this.isBlockedByBackendTruth()) {
      return true;
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
      this.executeCommand(
        this.casesService.submitInitialReview(this.caseId),
        result => result.rereviewRequired ? '初審已提交，案件已送往複審。' : '初審已提交。'
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
        this.buildInitialPayload(baseSubmissionId).pipe(
          switchMap(payload => this.casesService.reviseInitialReview(this.caseId, baseSubmissionId, payload))
        ),
        () => '初審修正已提交。'
      );
    }
  }

  onSecondaryAction(): void {
    if (this.commandSecondaryDisabled) {
      return;
    }

    const editableSubmissionId = this.resolveInitialEditableSubmissionId();
    if (!editableSubmissionId) {
      this.setCommandFeedback('無法取得可保存的初審草稿。', 'blocked');
      return;
    }

    this.executeCommand(
      this.buildInitialPayload(editableSubmissionId).pipe(
        switchMap(payload => this.casesService.saveInitialReviewDraft(this.caseId, payload))
      ),
      () => '初審草稿已保存。'
    );
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

  private buildInitialPayload(submissionId: string): Observable<InitialReviewDraftPayload> {
    return this.casesService.getSubmissionDetail(submissionId).pipe(
      map(detail => this.mapInitialPayload(detail))
    );
  }

  private mapInitialPayload(detail: SubmissionDetailViewModel): InitialReviewDraftPayload {
    return {
      answers: detail.answers.map(answer => ({
        itemId: answer.itemId,
        answerValue: answer.answerValue
      })),
      comment: detail.comment ?? null
    };
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

  private executeCommand(command$: Observable<CommandResultViewModel>, successMessage: (result: CommandResultViewModel) => string): void {
    this.commandPending = true;
    this.commandFeedbackMessage = '';

    command$.pipe(finalize(() => {
      this.commandPending = false;
    })).subscribe({
      next: result => {
        this.setCommandFeedback(successMessage(result), 'info');
        this.reloadCaseDetail();
      },
      error: () => {
        this.setCommandFeedback('初審 command 執行失敗，limited wiring 僅提供最小錯誤提示。', 'blocked');
      }
    });
  }

  private reloadCaseDetail(): void {
    this.casesService.getCaseDetail(this.caseId).subscribe({
      next: detail => this.applyCaseDetail(detail),
      error: () => this.setCommandFeedback('重新整理初審頁面 bootstrap 失敗。', 'blocked')
    });
  }

  private setCommandFeedback(message: string, tone: 'info' | 'blocked'): void {
    this.commandFeedbackMessage = message;
    this.commandFeedbackTone = tone;
  }
}
