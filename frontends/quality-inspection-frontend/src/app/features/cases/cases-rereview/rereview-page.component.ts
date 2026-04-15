import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { SubmissionRef } from '../components/review-version-summary-region/review-version-summary-region.component';

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

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.caseId = this.route.snapshot.paramMap.get('caseId') ?? 'CASE-WAVE5';
    this.applyContext(this.createDefaultContext(this.caseId));
  }

  applyContext(context: RereviewContext): void {
    this.context = context;
    this.caseId = context.caseId;
    this.resolveMode(context);
  }

  resolveMode(context: RereviewContext): void {
    if (context.isClosedRevisionException) {
      this.mode = 'CLOSED_REVISION_EXCEPTION';
      this.modeLabel = 'CLOSED_REVISION_EXCEPTION';
      this.bannerTone = 'exception-amber';
      this.bannerMessage = '此案件已結案，但目前以例外修正骨架呈現。';
      return;
    }

    if (context.blockedReason || ['CLOSED', 'CANCELLED'].includes(context.status)) {
      this.mode = 'READONLY_BLOCKED';
      this.modeLabel = 'READONLY_BLOCKED';
      this.bannerTone = 'blocked';
      this.bannerMessage = '案件目前不可執行複審作業。';
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

  get showExceptionBanner(): boolean {
    return this.mode === 'CLOSED_REVISION_EXCEPTION';
  }

  get showBlockedBanner(): boolean {
    return this.mode === 'READONLY_BLOCKED';
  }

  get showVersionSummary(): boolean {
    return this.mode === 'REVISION' || this.mode === 'CLOSED_REVISION_EXCEPTION';
  }

  get commandPrimaryLabel(): string {
    if (this.mode === 'CLOSED_REVISION_EXCEPTION') {
      return 'C16 placeholder label';
    }

    if (this.mode === 'REVISION') {
      return '提交複審修正 placeholder';
    }

    if (this.mode === 'EDIT_DRAFT') {
      return '儲存複審草稿 placeholder';
    }

    return '提交複審 placeholder';
  }

  private createDefaultContext(caseId: string): RereviewContext {
    return {
      caseId,
      caseTitle: '複審作業骨架',
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
}
