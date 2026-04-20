import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, Observable } from 'rxjs';

import {
  AssignmentCandidateViewModel,
  AssignmentContextViewModel,
  AssignRereviewerPayload,
  AssignReviewerPayload,
  CasesService,
  CommandResultViewModel
} from '../services/cases.service';

export type CaseAssignmentMode =
  | 'ASSIGN_INITIAL'
  | 'REASSIGN_INITIAL'
  | 'MIXED_ASSIGNMENT'
  | 'READONLY_BLOCKED';

export interface CaseAssignmentContext {
  caseId: string;
  caseTitle: string;
  status: string;
  stageLabel: string;
  allowedActions: string[];
  blockedReason: string | null;
  blocked: boolean;
  reviewerName?: string;
  rereviewerName?: string;
  reviewerDueAt?: string;
  rereviewerDueAt?: string;
  effectiveDueAt?: string;
}

@Component({
  selector: 'app-case-assignment-page',
  templateUrl: './case-assignment-page.component.html',
  styleUrls: ['./case-assignment-page.component.scss']
})
export class CaseAssignmentPageComponent implements OnInit {
  caseId = '';
  context: CaseAssignmentContext = this.createDefaultContext('');
  mode: CaseAssignmentMode = 'ASSIGN_INITIAL';
  modeLabel = '待初審指派';
  bannerMessage = '';
  commandPending = false;
  loading = false;
  reviewerLoading = false;
  rereviewerLoading = false;
  commandFeedbackMessage = '';
  commandFeedbackTone: 'info' | 'blocked' = 'info';
  reviewerKeyword = '';
  rereviewerKeyword = '';
  reviewerCandidates: AssignmentCandidateViewModel[] = [];
  rereviewerCandidates: AssignmentCandidateViewModel[] = [];
  selectedReviewerId: string | null = null;
  selectedRereviewerId: string | null = null;
  reviewerDueAtInput = '';
  rereviewerDueAtInput = '';
  private assignmentContext?: AssignmentContextViewModel;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly casesService: CasesService
  ) {}

  ngOnInit(): void {
    this.caseId = this.route.snapshot.paramMap.get('caseId') ?? 'CASE-WAVE5';
    this.loadAssignmentPage();
  }

  applyContext(context: CaseAssignmentContext, assignmentContext?: AssignmentContextViewModel): void {
    this.context = context;
    this.caseId = context.caseId;
    this.assignmentContext = assignmentContext;
    this.resolveMode(context, assignmentContext);
  }

  resolveMode(context: CaseAssignmentContext, assignmentContext?: AssignmentContextViewModel): void {
    const backendMode = assignmentContext?.pageMode;

    if (backendMode === 'READONLY_BLOCKED' || context.blocked || ['CLOSED', 'CANCELLED'].includes(context.status)) {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = '不可指派';
      this.bannerMessage = this.resolveBlockedBannerMessage(context.blockedReason);
      return;
    }

    if (backendMode === 'MIXED_ASSIGNMENT') {
      this.mode = 'MIXED_ASSIGNMENT';
      this.modeLabel = '複審指派';
      this.bannerMessage = '';
      return;
    }

    if (backendMode === 'REASSIGN_INITIAL') {
      this.mode = 'REASSIGN_INITIAL';
      this.modeLabel = '初審改派';
      this.bannerMessage = '';
      return;
    }

    this.mode = 'ASSIGN_INITIAL';
    this.modeLabel = '待初審指派';
    this.bannerMessage = '';
  }

  get showBlockedBanner(): boolean {
    return this.mode === 'READONLY_BLOCKED';
  }

  get showCommandFeedback(): boolean {
    return this.commandFeedbackMessage.length > 0;
  }

  get showReviewerSection(): boolean {
    return true;
  }

  get showRereviewerSection(): boolean {
    return true;
  }

  get showSubmitZone(): boolean {
    return this.mode !== 'READONLY_BLOCKED';
  }

  get showReviewerInputs(): boolean {
    return this.mode !== 'READONLY_BLOCKED' && this.mode !== 'MIXED_ASSIGNMENT' && this.isReviewerActionEnabled;
  }

  get showRereviewerInputs(): boolean {
    return this.mode !== 'READONLY_BLOCKED' && this.mode === 'MIXED_ASSIGNMENT' && this.isRereviewerActionEnabled;
  }

  get statusDisplayLabel(): string {
    return this.resolveStatusLabel(this.context.status);
  }

  get modeBannerTitle(): string {
    switch (this.mode) {
      case 'REASSIGN_INITIAL':
        return '初審改派';
      case 'MIXED_ASSIGNMENT':
        return '複審指派';
      case 'READONLY_BLOCKED':
        return '指派已封鎖';
      default:
        return '初審指派';
    }
  }

  get modeBannerCopy(): string {
    switch (this.mode) {
      case 'REASSIGN_INITIAL':
        return '此案件已有初審醫師，可重新指定新的初審責任人與截止日。';
      case 'MIXED_ASSIGNMENT':
        return '初審責任人已固定，當前任務是完成複審醫師指派與截止日設定。';
      case 'READONLY_BLOCKED':
        return '此案件目前不可做任何指派或改派，頁面只保留摘要與返回動作。';
      default:
        return '請先完成初審醫師與截止日設定，再讓案件進入待初審流程。';
    }
  }

  get summaryItems(): Array<{ label: string; value: string }> {
    return [
      { label: '批次編號', value: this.assignmentContext?.batchId ?? '尚未提供' },
      { label: '院區 / 病歷類型', value: `${this.assignmentContext?.hospitalId ?? '未提供'} / ${this.assignmentContext?.medicalRecordType ?? '未提供'}` },
      { label: '目前狀態', value: this.statusDisplayLabel },
      { label: '目前截止日', value: this.context.effectiveDueAt || '尚未設定' }
    ];
  }

  get reviewerReadonlyHint(): string {
    if (this.mode === 'READONLY_BLOCKED') {
      return '此案件目前只提供初審資訊摘要，不開放指派或改派。';
    }

    if (this.mode === 'MIXED_ASSIGNMENT') {
      return '初審醫師已固定，本頁此階段不再調整初審責任人。';
    }

    return '目前沒有可執行的初審指派動作。';
  }

  get rereviewerReadonlyHint(): string {
    if (this.mode === 'READONLY_BLOCKED') {
      return '此案件目前只提供複審資訊摘要，不開放指派或改派。';
    }

    if (this.mode !== 'MIXED_ASSIGNMENT') {
      return '案件尚未進入複審指派階段，先完成初審流程後再處理。';
    }

    return '目前沒有可執行的複審指派動作。';
  }

  get currentDueDateLabel(): string {
    if (this.context.effectiveDueAt) {
      return `目前截止日：${this.context.effectiveDueAt}`;
    }

    if (this.mode === 'MIXED_ASSIGNMENT') {
      return '目前未設定複審截止日。';
    }

    return '目前未設定初審截止日。';
  }

  get dueDateInputLabel(): string {
    return this.mode === 'MIXED_ASSIGNMENT' ? '複審截止日' : '初審截止日';
  }

  get activeDueAtInput(): string {
    return this.mode === 'MIXED_ASSIGNMENT' ? this.rereviewerDueAtInput : this.reviewerDueAtInput;
  }

  get reviewerSearchDisabled(): boolean {
    return this.commandPending || this.reviewerLoading || !this.isReviewerActionEnabled;
  }

  get rereviewerSearchDisabled(): boolean {
    return this.commandPending || this.rereviewerLoading || !this.isRereviewerActionEnabled;
  }

  get isReviewerActionEnabled(): boolean {
    return this.hasAllowedAction('ASSIGN_REVIEWER') || this.hasAllowedAction('REASSIGN_REVIEWER');
  }

  get isRereviewerActionEnabled(): boolean {
    return this.hasAllowedAction('ASSIGN_REREVIEWER') || this.hasAllowedAction('REASSIGN_REREVIEWER');
  }

  get primaryActionDisabled(): boolean {
    if (this.commandPending || this.showBlockedBanner) {
      return true;
    }

    if (this.mode === 'MIXED_ASSIGNMENT') {
      return !this.isRereviewerActionEnabled || !this.selectedRereviewerId || !this.hasValidDueAtInput(this.rereviewerDueAtInput);
    }

    return !this.isReviewerActionEnabled || !this.selectedReviewerId || !this.hasValidDueAtInput(this.reviewerDueAtInput);
  }

  get primaryActionLabel(): string {
    if (this.mode === 'MIXED_ASSIGNMENT') {
      return this.hasAllowedAction('REASSIGN_REREVIEWER') ? '提交複審改派' : '提交複審指派';
    }

    return this.hasAllowedAction('REASSIGN_REVIEWER') ? '提交初審改派' : '提交初審指派';
  }

  trackCandidate(_: number, candidate: AssignmentCandidateViewModel): string {
    return `${candidate.role}:${candidate.userId}`;
  }

  selectReviewerCandidate(candidate: AssignmentCandidateViewModel): void {
    if (!candidate.selectable || !this.isReviewerActionEnabled) {
      return;
    }

    this.selectedReviewerId = candidate.userId;
    this.commandFeedbackMessage = '';
  }

  selectRereviewerCandidate(candidate: AssignmentCandidateViewModel): void {
    if (!candidate.selectable || !this.isRereviewerActionEnabled) {
      return;
    }

    this.selectedRereviewerId = candidate.userId;
    this.commandFeedbackMessage = '';
  }

  onSearchReviewerCandidates(): void {
    this.loadReviewerCandidates();
  }

  onSearchRereviewerCandidates(): void {
    this.loadRereviewerCandidates();
  }

  onDueAtInputChange(value: string): void {
    if (this.mode === 'MIXED_ASSIGNMENT') {
      this.rereviewerDueAtInput = value;
      return;
    }

    this.reviewerDueAtInput = value;
  }

  onPrimaryAction(): void {
    if (this.primaryActionDisabled) {
      return;
    }

    if (this.mode === 'MIXED_ASSIGNMENT') {
      const rereviewerId = this.selectedRereviewerId;
      if (!rereviewerId) {
        this.setCommandFeedback('請先選擇複審醫師。', 'blocked');
        return;
      }

      const dueAt = this.normalizeDueAtInput(this.rereviewerDueAtInput);
      if (!dueAt) {
        this.setCommandFeedback('請先設定複審截止日。', 'blocked');
        return;
      }

      const payload: AssignRereviewerPayload = {
        rereviewerId,
        dueAt
      };
      this.executeCommand(
        this.hasAllowedAction('REASSIGN_REREVIEWER')
          ? this.casesService.reassignRereviewer(this.caseId, payload)
          : this.casesService.assignRereviewer(this.caseId, payload),
        () => this.hasAllowedAction('REASSIGN_REREVIEWER') ? '複審改派已送出。' : '複審指派已送出。'
      );
      return;
    }

    const reviewerId = this.selectedReviewerId;
    if (!reviewerId) {
      this.setCommandFeedback('請先選擇初審醫師。', 'blocked');
      return;
    }

    const dueAt = this.normalizeDueAtInput(this.reviewerDueAtInput);
    if (!dueAt) {
      this.setCommandFeedback('請先設定初審截止日。', 'blocked');
      return;
    }

    const payload: AssignReviewerPayload = {
      reviewerId,
      dueAt
    };
    this.executeCommand(
      this.hasAllowedAction('REASSIGN_REVIEWER')
        ? this.casesService.reassignReviewer(this.caseId, payload)
        : this.casesService.assignReviewer(this.caseId, payload),
      () => this.hasAllowedAction('REASSIGN_REVIEWER') ? '初審改派已送出。' : '初審指派已送出。'
    );
  }

  onBackToCaseDetail(): void {
    this.router.navigate(['/cases', this.caseId]);
  }

  private createDefaultContext(caseId: string): CaseAssignmentContext {
    return {
      caseId,
      caseTitle: '案件分派頁',
      status: 'SELECTED_PENDING_ASSIGNMENT',
      stageLabel: '待分派',
      allowedActions: [],
      blockedReason: null,
      blocked: false
    };
  }

  private loadAssignmentPage(): void {
    this.loading = true;
    this.casesService.getAssignmentContext(this.caseId)
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: context => {
          this.applyAssignmentContext(context);
          this.loadReviewerCandidates();
          this.loadRereviewerCandidates();
        },
        error: () => {
          this.applyContext({
            ...this.createDefaultContext(this.caseId),
            status: 'CANCELLED',
            stageLabel: '載入失敗',
            blocked: true,
            blockedReason: 'ASSIGNMENT_CONTEXT_LOAD_FAILED'
          });
        }
      });
  }

  private applyAssignmentContext(context: AssignmentContextViewModel): void {
    this.applyContext({
      caseId: context.caseId,
      caseTitle: '案件分派頁',
      status: context.status,
      stageLabel: this.resolveStageLabel(context.status),
      allowedActions: context.allowedActions,
      blockedReason: context.blockedReason ?? null,
      blocked: context.blocked,
      reviewerName: context.currentReviewer?.displayName,
      rereviewerName: context.currentRereviewer?.displayName,
      reviewerDueAt: context.reviewerDueAt,
      rereviewerDueAt: context.rereviewerDueAt,
      effectiveDueAt: context.effectiveDueAt
    }, context);

    this.reviewerDueAtInput = this.toDateTimeLocalInput(context.reviewerDueAt ?? context.effectiveDueAt);
    this.rereviewerDueAtInput = this.toDateTimeLocalInput(context.rereviewerDueAt ?? context.effectiveDueAt);
  }

  private loadReviewerCandidates(): void {
    this.reviewerLoading = true;
    this.casesService.getAssignmentCandidates(this.caseId, 'REVIEWER', this.reviewerKeyword)
      .pipe(finalize(() => {
        this.reviewerLoading = false;
      }))
      .subscribe({
        next: candidates => {
          this.reviewerCandidates = candidates;
          if (!this.selectedReviewerId || !candidates.some(item => item.userId === this.selectedReviewerId && item.selectable)) {
            this.selectedReviewerId = candidates.find(item => item.selectable)?.userId ?? null;
          }
        },
        error: () => {
          this.reviewerCandidates = [];
          this.selectedReviewerId = null;
        }
      });
  }

  private loadRereviewerCandidates(): void {
    this.rereviewerLoading = true;
    this.casesService.getAssignmentCandidates(this.caseId, 'REREVIEWER', this.rereviewerKeyword)
      .pipe(finalize(() => {
        this.rereviewerLoading = false;
      }))
      .subscribe({
        next: candidates => {
          this.rereviewerCandidates = candidates;
          if (!this.selectedRereviewerId || !candidates.some(item => item.userId === this.selectedRereviewerId && item.selectable)) {
            this.selectedRereviewerId = candidates.find(item => item.selectable)?.userId ?? null;
          }
        },
        error: () => {
          this.rereviewerCandidates = [];
          this.selectedRereviewerId = null;
        }
      });
  }

  private resolveStageLabel(status: string): string {
    switch (status) {
      case 'PENDING_INITIAL_REVIEW':
        return '待初審';
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
        return '待分派';
    }
  }

  private resolveStatusLabel(status: string): string {
    switch (status) {
      case 'SELECTED_PENDING_ASSIGNMENT':
        return '待分派';
      case 'PENDING_INITIAL_REVIEW':
        return '待初審';
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
        return '狀態待確認';
    }
  }

  private resolveBlockedBannerMessage(blockedReason: string | null): string {
    switch (blockedReason) {
      case 'CASE_ALREADY_CLOSED':
        return '此案件已結案，不能進行分派或改派。';
      case 'CASE_ALREADY_CANCELLED':
        return '此案件已取消，不能進行分派或改派。';
      case 'CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS':
        return '此案件目前狀態不允許分派或改派。';
      case 'ASSIGNMENT_CONTEXT_LOAD_FAILED':
        return '案件分派資料載入失敗，請稍後重試或返回案件詳情。';
      default:
        return '此案件目前不可進行分派或改派。';
    }
  }

  formatCandidateBlockedReason(blockedReason: string | null | undefined): string {
    switch (blockedReason) {
      case 'CANDIDATE_INACTIVE':
        return '此候選人目前未啟用，不能指派。';
      case 'REREVIEWER_SAME_AS_REVIEWER':
        return '複審醫師不可與目前初審醫師相同。';
      case 'HOSPITAL_SCOPE_MISMATCH':
        return '此候選人不在目前院區或授權範圍內。';
      default:
        return blockedReason ?? '';
    }
  }

  private hasAllowedAction(action: string): boolean {
    return this.assignmentContext?.allowedActions.includes(action) ?? false;
  }

  private executeCommand(command$: Observable<CommandResultViewModel>, successMessageFactory: (result: CommandResultViewModel) => string): void {
    this.commandPending = true;
    command$
      .pipe(finalize(() => {
        this.commandPending = false;
      }))
      .subscribe({
        next: result => {
          this.setCommandFeedback(successMessageFactory(result), 'info');
          this.loadAssignmentPage();
        },
        error: () => {
          this.setCommandFeedback('案件分派執行失敗，請確認指派資訊後再試。', 'blocked');
        }
      });
  }

  private setCommandFeedback(message: string, tone: 'info' | 'blocked'): void {
    this.commandFeedbackMessage = message;
    this.commandFeedbackTone = tone;
  }

  private hasValidDueAtInput(value: string): boolean {
    return this.normalizeDueAtInput(value) !== null;
  }

  private normalizeDueAtInput(value: string): string | null {
    const trimmedValue = value.trim();
    if (!trimmedValue) {
      return null;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmedValue)) {
      return `${trimmedValue}:00`;
    }

    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(trimmedValue)) {
      return trimmedValue;
    }

    return null;
  }

  private toDateTimeLocalInput(value?: string): string {
    if (!value) {
      return '';
    }

    const matchedValue = value.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})/);
    return matchedValue ? matchedValue[1] : '';
  }
}
