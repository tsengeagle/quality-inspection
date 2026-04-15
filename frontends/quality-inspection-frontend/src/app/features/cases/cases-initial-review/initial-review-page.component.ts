import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { SubmissionRef } from '../components/review-version-summary-region/review-version-summary-region.component';

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

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.caseId = this.route.snapshot.paramMap.get('caseId') ?? 'CASE-WAVE5';
    this.applyContext(this.createDefaultContext(this.caseId));
  }

  applyContext(context: InitialReviewContext): void {
    this.context = context;
    this.caseId = context.caseId;
    this.resolveMode(context);
  }

  resolveMode(context: InitialReviewContext): void {
    if (context.blockedReason || ['CLOSED', 'CANCELLED'].includes(context.status)) {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerMessage = '案件目前不可執行初審作業。';
      return;
    }

    if (context.hasSubmittedBase) {
      this.mode = 'REVISION';
      this.modeLabel = 'REVISION';
      this.bannerMessage = '';
      return;
    }

    if (context.hasDraft) {
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

  get commandPrimaryLabel(): string {
    switch (this.mode) {
      case 'EDIT_DRAFT':
        return '儲存草稿 placeholder';
      case 'REVISION':
        return '提交初審修正 placeholder';
      default:
        return '提交初審 placeholder';
    }
  }

  get commandSecondaryLabel(): string | undefined {
    return this.mode === 'READONLY_BLOCKED' ? undefined : '返回案件詳情';
  }

  private createDefaultContext(caseId: string): InitialReviewContext {
    return {
      caseId,
      caseTitle: '初審作業骨架',
      status: 'PENDING_INITIAL_REVIEW',
      stageLabel: '待初審',
      blockedReason: null,
      hasDraft: false,
      hasSubmittedBase: false
    };
  }
}
