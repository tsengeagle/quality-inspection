import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
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
  modeLabel = 'ASSIGN_INITIAL';
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
  private assignmentContext?: AssignmentContextViewModel;

  constructor(
    private readonly route: ActivatedRoute,
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
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerMessage = this.resolveBlockedBannerMessage(context.blockedReason);
      return;
    }

    if (backendMode === 'MIXED_ASSIGNMENT') {
      this.mode = 'MIXED_ASSIGNMENT';
      this.modeLabel = 'MIXED_ASSIGNMENT';
      this.bannerMessage = '';
      return;
    }

    if (backendMode === 'REASSIGN_INITIAL') {
      this.mode = 'REASSIGN_INITIAL';
      this.modeLabel = 'REASSIGN_INITIAL';
      this.bannerMessage = '';
      return;
    }

    this.mode = 'ASSIGN_INITIAL';
    this.modeLabel = 'ASSIGN_INITIAL';
    this.bannerMessage = '';
  }

  get showBlockedBanner(): boolean {
    return this.mode === 'READONLY_BLOCKED';
  }

  get showCommandFeedback(): boolean {
    return this.commandFeedbackMessage.length > 0;
  }

  get showReviewerSection(): boolean {
    return this.mode !== 'READONLY_BLOCKED';
  }

  get showRereviewerSection(): boolean {
    return this.mode === 'MIXED_ASSIGNMENT';
  }

  get showSubmitZone(): boolean {
    return this.mode !== 'READONLY_BLOCKED';
  }

  get currentDueDateLabel(): string {
    if (this.context.effectiveDueAt) {
      return `目前截止日：${this.context.effectiveDueAt}`;
    }

    if (this.mode === 'MIXED_ASSIGNMENT') {
      return '目前未提供複審截止日；helper slot 仍維持 limited boundary。';
    }

    return '目前未提供初審截止日；helper slot 仍維持 limited boundary。';
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
      return !this.isRereviewerActionEnabled || !this.selectedRereviewerId;
    }

    return !this.isReviewerActionEnabled || !this.selectedReviewerId;
  }

  get primaryActionLabel(): string {
    if (this.mode === 'MIXED_ASSIGNMENT') {
      return this.hasAllowedAction('REASSIGN_REREVIEWER') ? '複審改派' : '複審指派';
    }

    return this.hasAllowedAction('REASSIGN_REVIEWER') ? '初審改派' : '初審指派';
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

      const payload: AssignRereviewerPayload = {
        rereviewerId,
        dueAt: this.context.rereviewerDueAt ?? this.context.effectiveDueAt ?? null
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

    const payload: AssignReviewerPayload = {
      reviewerId,
      dueAt: this.context.reviewerDueAt ?? this.context.effectiveDueAt ?? null
    };
    this.executeCommand(
      this.hasAllowedAction('REASSIGN_REVIEWER')
        ? this.casesService.reassignReviewer(this.caseId, payload)
        : this.casesService.assignReviewer(this.caseId, payload),
      () => this.hasAllowedAction('REASSIGN_REVIEWER') ? '初審改派已送出。' : '初審指派已送出。'
    );
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

  private resolveBlockedBannerMessage(blockedReason: string | null): string {
    switch (blockedReason) {
      case 'CASE_ALREADY_CLOSED':
        return '此案件已結案，不能進行分派或改派。';
      case 'CASE_ALREADY_CANCELLED':
        return '此案件已取消，不能進行分派或改派。';
      case 'CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS':
        return '此案件目前狀態不允許分派或改派。';
      case 'ASSIGNMENT_CONTEXT_LOAD_FAILED':
        return '案件分派資料載入失敗，limited integration 僅提供最小阻擋提示。';
      default:
        return '此案件目前不可進行分派或改派。';
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
          this.setCommandFeedback('案件分派 command 執行失敗，limited integration 僅提供最小錯誤提示。', 'blocked');
        }
      });
  }

  private setCommandFeedback(message: string, tone: 'info' | 'blocked'): void {
    this.commandFeedbackMessage = message;
    this.commandFeedbackTone = tone;
  }
}
