import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CasesService, CaseDetailViewModel } from '../services/cases.service';

/** C-03 CaseDetailPage mode 列舉 */
export type CaseDetailMode =
  | 'FULL_CONTROL_CENTER'
  | 'METADATA_CONTROL_CENTER'
  | 'REVIEW_ACCESS'
  | 'READONLY_TERMINAL';

/** C-03 — UI-010 CaseDetailPageComponent
 *  A2 control center — 案件生命週期控制中心。
 *  4-mode: FULL_CONTROL_CENTER / METADATA_CONTROL_CENTER / REVIEW_ACCESS / READONLY_TERMINAL
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private casesService: CasesService
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
    } else if (d.role === 'MANAGER' || d.role === 'ADMIN') {
      this.mode = 'FULL_CONTROL_CENTER';
    } else if ((d.role === 'REVIEWER' || d.role === 'REREVIEWER') && d.isOwner) {
      this.mode = 'REVIEW_ACCESS';
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
      case 'REVIEW_ACCESS':
        this.modeLabel = '審查存取';
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

