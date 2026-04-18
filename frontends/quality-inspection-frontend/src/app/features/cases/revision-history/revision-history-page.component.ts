import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CasesService, RevisionHistoryViewModel } from '../services/cases.service';

import { ActorContextService } from '@core/actor-context/actor-context.service';

/** C-11 RevisionHistoryPage mode 列舉 */
export type RevisionHistoryMode =
  | 'FULL_HISTORY'
  | 'METADATA_HISTORY'
  | 'READONLY_TERMINAL';

/** Stage 篩選 tabs */
export type RevisionStageTab = 'ALL' | 'INITIAL_ONLY' | 'REREVIEW_ONLY';

/** C-11 — UI-014 RevisionHistoryPageComponent
 *  3-mode: FULL_HISTORY / METADATA_HISTORY / READONLY_TERMINAL
 *  HR-1: METADATA_HISTORY → C21 顯示 "詳細差異內容不適用於此視角"
 *  HR-2: C22 必須是 outlined warning-tone button，不能 fixed
 *  hasExceptionEntry: CLOSED + originalRereviewer + baseSubmission → 顯示 C22 navigate button */
@Component({
  selector: 'app-revision-history-page',
  templateUrl: './revision-history-page.component.html',
  styleUrls: ['./revision-history-page.component.scss']
})
export class RevisionHistoryPageComponent implements OnInit {

  caseId = '';
  vm: RevisionHistoryViewModel | null = null;

  mode: RevisionHistoryMode = 'READONLY_TERMINAL';
  stageTab: RevisionStageTab = 'ALL';

  /** C03 modeLabel */
  modeLabel = '';

  showBanner = false;
  bannerTone: 'blocked' | 'exception-amber' | 'info' = 'info';
  bannerMessage = '';

  /** HR-1: METADATA_HISTORY → diff 不適用訊息 */
  showMetadataOnlyNotice = false;

  /** HR-2 / exception entry: CLOSED + originalRereviewer + baseSubmission */
  hasExceptionEntry = false;

  private hasExplicitStageTab = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private casesService: CasesService,
    private actorContextService: ActorContextService
  ) {}

  ngOnInit(): void {
    this.caseId = this.route.snapshot.paramMap.get('caseId') ?? '';
    const savedTab = this.route.snapshot.queryParamMap.get('stageTab');
    if (savedTab && ['ALL', 'INITIAL_ONLY', 'REREVIEW_ONLY'].includes(savedTab)) {
      this.stageTab = savedTab as RevisionStageTab;
      this.hasExplicitStageTab = true;
    }

    this.casesService.getRevisionHistory(this.caseId).subscribe(vm => {
      this.vm = vm;
      this._applyRecommendedStageTab();
      this._resolveMode();
    });
  }

  private _applyRecommendedStageTab(): void {
    if (this.hasExplicitStageTab || !this.vm) {
      return;
    }

    switch (this.actorContextService.toCanonicalRole(this.vm.actorRole)) {
      case 'REVIEWER':
        this.stageTab = 'INITIAL_ONLY';
        break;
      case 'REREVIEWER':
        this.stageTab = 'REREVIEW_ONLY';
        break;
      default:
        this.stageTab = 'ALL';
        break;
    }
  }

  /** 3-mode resolution */
  private _resolveMode(): void {
    const d = this.vm;
    if (!d) {
      this.mode = 'READONLY_TERMINAL';
      this._applyMode();
      return;
    }

    if (d.projectionType === 'METADATA_ONLY') {
      this.mode = 'METADATA_HISTORY';
    } else if (['CLOSED', 'CANCELLED'].includes(d.status)) {
      this.mode = 'READONLY_TERMINAL';
    } else {
      this.mode = 'FULL_HISTORY';
    }

    this._applyMode();
  }

  private _applyMode(): void {
    const d = this.vm;

    switch (this.mode) {
      case 'FULL_HISTORY':
        this.modeLabel = '完整歷程';
        this.showMetadataOnlyNotice = false;
        this.showBanner = false;
        // hasExceptionEntry: CLOSED + originalRereviewer + baseSubmission（FULL_HISTORY 不符，不顯示）
        this.hasExceptionEntry = false;
        break;

      case 'METADATA_HISTORY':
        this.modeLabel = 'Metadata 歷程';
        // HR-1
        this.showMetadataOnlyNotice = true;
        this.showBanner = false;
        this.hasExceptionEntry = false;
        break;

      case 'READONLY_TERMINAL':
        this.modeLabel = '唯讀歷程（終止狀態）';
        this.showMetadataOnlyNotice = false;
        this.hasExceptionEntry = !!d?.hasExceptionEntry;
        this.showBanner = true;
        this.bannerTone = this.hasExceptionEntry ? 'exception-amber' : 'info';
        this.bannerMessage = this.hasExceptionEntry
          ? '此案件已結案，但符合例外複審修正導頁條件。'
          : '此案件處於終止狀態，僅可檢視歷程。';
        break;
    }
  }

  onStageTabChange(tab: RevisionStageTab): void {
    this.stageTab = tab;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { stageTab: tab },
      queryParamsHandling: 'merge'
    });
  }

  /** C22: exception mode → 前往複審作業頁（HR-2: navigate-only） */
  navigateToRereview(): void {
    this.router.navigate(['/cases', this.caseId, 'rereview']);
  }
}
