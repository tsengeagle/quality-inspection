import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

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
  reviewerName?: string;
  rereviewerName?: string;
  reviewerAssigned: boolean;
  rereviewStageActive: boolean;
  blockedReason: string | null;
  dueDateHint: string;
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

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.caseId = this.route.snapshot.paramMap.get('caseId') ?? 'CASE-WAVE5';
    this.applyContext(this.createDefaultContext(this.caseId));
  }

  applyContext(context: CaseAssignmentContext): void {
    this.context = context;
    this.caseId = context.caseId;
    this.resolveMode(context);
  }

  resolveMode(context: CaseAssignmentContext): void {
    if (context.blockedReason || ['CLOSED', 'CANCELLED'].includes(context.status)) {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerMessage = '此案件目前不可進行分派或改派。';
      return;
    }

    if (context.rereviewStageActive) {
      this.mode = 'MIXED_ASSIGNMENT';
      this.modeLabel = 'MIXED_ASSIGNMENT';
      this.bannerMessage = '';
      return;
    }

    if (context.reviewerAssigned) {
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

  get showReviewerSection(): boolean {
    return this.mode !== 'READONLY_BLOCKED';
  }

  get showRereviewerSection(): boolean {
    return this.mode === 'MIXED_ASSIGNMENT';
  }

  get showSubmitZone(): boolean {
    return this.mode !== 'READONLY_BLOCKED';
  }

  get primaryActionLabel(): string {
    switch (this.mode) {
      case 'REASSIGN_INITIAL':
        return '初審改派 placeholder';
      case 'MIXED_ASSIGNMENT':
        return '複審指派 placeholder';
      default:
        return '初審指派 placeholder';
    }
  }

  private createDefaultContext(caseId: string): CaseAssignmentContext {
    return {
      caseId,
      caseTitle: '案件分派骨架',
      status: 'SELECTED_PENDING_ASSIGNMENT',
      stageLabel: '待分派',
      reviewerAssigned: false,
      rereviewStageActive: false,
      blockedReason: null,
      dueDateHint: '截止日 helper slot 目前僅提供 placeholder，不含互動。'
    };
  }
}
