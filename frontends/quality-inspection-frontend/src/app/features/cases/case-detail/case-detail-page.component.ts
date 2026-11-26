import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CasesService, CaseDetailViewModel } from '../services/cases.service';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CanonicalActorRole } from '@core/actor-context/actor-context.model';

/** C-03 CaseDetailPage mode 列舉 */
export type CaseDetailMode =
  | 'FULL_CONTROL_CENTER'
  | 'METADATA_CONTROL_CENTER'
  | 'ROLE_LIMITED'
  | 'READONLY_TERMINAL';

/** C-03 — UI-010 CaseDetailPageComponent
 *  A2 control center — 案件生命週期控制中心。
 *  4-mode: FULL_CONTROL_CENTER / METADATA_CONTROL_CENTER / ROLE_LIMITED / READONLY_TERMINAL
 *  Mode 由 container 計算（role + caseState + ownership + projectionType），絕不從 URL 讀取。 */
@Component({
  selector: 'app-case-detail-page',
  templateUrl: './case-detail-page.component.html',
  styleUrls: ['./case-detail-page.component.scss']
})
export class CaseDetailPageComponent implements OnInit {

  caseId = '';
  caseDetail: CaseDetailViewModel | null = null;

  /** 4-mode resolution 結果 */
  mode: CaseDetailMode = 'READONLY_TERMINAL';

  /** C02 banner */
  bannerTone: 'blocked' | 'exception-amber' | 'info' = 'blocked';
  bannerMessage = '';
  showBanner = false;

  /** C03 modeLabel */
  modeLabel = '';

  /** C04 showProjectionNotice */
  showProjectionNotice = false;

  /** READONLY_TERMINAL — 是否為 terminal 狀態 */
  get isTerminal(): boolean {
    return this.mode === 'READONLY_TERMINAL';
  }

  get actorRole(): string {
    return this.caseDetail?.actorRole ?? '';
  }

  get canonicalActorRole(): CanonicalActorRole | null {
    return this.actorContextService.toCanonicalRole(this.caseDetail?.actorRole);
  }

  get caseTitle(): string {
    return '案件詳情';
  }

  get stageLabel(): string {
    return this.caseDetail?.status ?? '';
  }

  get initialSummary(): string | undefined {
    const initialSubmission = this.caseDetail?.submissions.find(item => item.stage === 'INITIAL');

    if (!initialSubmission) {
      return undefined;
    }

    return `第 ${initialSubmission.revisionNo} 版 / ${initialSubmission.status}`;
  }

  get rereviewSummary(): string | undefined {
    const latestRereview = this.caseDetail?.submissions.find(item => item.submissionId === this.caseDetail?.latestRereviewSubmissionId)
      ?? this.caseDetail?.submissions.find(item => item.stage === 'REREVIEW');

    if (!latestRereview) {
      if (this.caseDetail?.finalScore != null) {
        return `FINAL / ${this.caseDetail.finalScore}`;
      }
      return undefined;
    }

    return `第 ${latestRereview.revisionNo} 版 / ${latestRereview.status}`;
  }

  get showInitialSummary(): boolean {
    if (this.mode === 'METADATA_CONTROL_CENTER') {
      return false;
    }

    if (this.mode === 'ROLE_LIMITED') {
      return this.canonicalActorRole === 'REVIEWER' || this.canonicalActorRole === 'REREVIEWER';
    }

    return true;
  }

  get showRereviewSummary(): boolean {
    if (this.mode === 'METADATA_CONTROL_CENTER') {
      return true;
    }

    if (this.mode === 'ROLE_LIMITED') {
      return this.canonicalActorRole === 'REREVIEWER';
    }

    return true;
  }

  get showLifecycleProgress(): boolean {
    return true;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private casesService: CasesService,
    private actorContextService: ActorContextService
  ) {}

  ngOnInit(): void {
    this.caseId = this.route.snapshot.paramMap.get('caseId') ?? '';
    this.casesService.getCaseDetail(this.caseId).subscribe(detail => {
      this.caseDetail = detail;
      this._resolveMode();
    });
  }

  /** 4-mode resolution: role + caseState + ownership + projectionType */
  private _resolveMode(): void {
    const d = this.caseDetail;
    if (!d) {
      this.mode = 'READONLY_TERMINAL';
      this._applyMode();
      return;
    }

    const isTerminalState = ['CLOSED', 'CANCELLED'].includes(d.status);

    if (d.projectionType === 'METADATA_ONLY') {
      this.mode = 'METADATA_CONTROL_CENTER';
    } else if (isTerminalState) {
      this.mode = 'READONLY_TERMINAL';
    } else if (this.actorContextService.isManagementRole(d.actorRole)) {
      this.mode = 'FULL_CONTROL_CENTER';
    } else if (d.isOwner || this.canonicalActorRole === 'AUDITOR') {
      this.mode = 'ROLE_LIMITED';
    } else {
      this.mode = 'READONLY_TERMINAL';
    }

    this._applyMode();
  }

  private _applyMode(): void {
    switch (this.mode) {
      case 'FULL_CONTROL_CENTER':
        this.modeLabel = '控制中心';
        this.showBanner = false;
        this.showProjectionNotice = false;
        break;
      case 'METADATA_CONTROL_CENTER':
        this.modeLabel = '控制中心（Metadata）';
        this.showBanner = false;
        this.showProjectionNotice = true;
        break;
      case 'ROLE_LIMITED':
        this.modeLabel = '受限檢視';
        this.showBanner = false;
        this.showProjectionNotice = false;
        break;
      case 'READONLY_TERMINAL':
        this.modeLabel = '唯讀（已結案/取消）';
        this.showBanner = true;
        this.bannerTone = 'blocked';
        this.bannerMessage = '此案件已進入終止狀態，無法執行命令。';
        this.showProjectionNotice = false;
        break;
    }
  }

  /** C08 navigate action */
  navigateTo(subPath: string): void {
    this.router.navigate(['/cases', this.caseId, subPath]);
  }
}

