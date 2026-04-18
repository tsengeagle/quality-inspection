import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CaseListResult, CasesService, CaseSummaryItem, CasesQueryCriteria } from '../services/cases.service';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CanonicalActorRole } from '@core/actor-context/actor-context.model';

/** C-01 CaseListPage UI state */
export interface CaseListUIState {
  tab: string;
  status: string;
  page: number;
  size: number;
}

/** C-01 — UI-009 CaseListPageComponent
 *  A1 work queue — 依 role 顯示對應預設 tab/filter；metadata-only mode 隱藏 business commands。
 *  Mode 由 container 計算（role + projectionType），絕不從 URL params 讀取。 */
@Component({
  selector: 'app-case-list-page',
  templateUrl: './case-list-page.component.html',
  styleUrls: ['./case-list-page.component.scss']
})
export class CaseListPageComponent implements OnInit {

  private readonly fallbackRole: CanonicalActorRole = 'REVIEWER';

  /** 正式 primary role 來源為 shared actor context，fallback 僅作過渡保護。 */
  role = this.fallbackRole;
  /** backend list response 提供的 projection truth */
  projectionType?: 'FULL' | 'METADATA_ONLY';

  /** 當前 UI state（驅動 filter/tab） */
  uiState: CaseListUIState = { tab: 'MY_QUEUE', status: 'PENDING', page: 0, size: 20 };

  /** mode 決定是否顯示 business commands（metadata-only → hidden） */
  showBusinessCommands = true;

  /** modeLabel 傳入 C03 */
  modeLabel = '';

  /** C04 showProjectionNotice */
  showProjectionNotice = false;

  /** 案件列表 */
  cases: CaseSummaryItem[] = [];

  get showManagedProjectsTab(): boolean {
    return this.actorContextService.isManagementRole(this.role)
      || this.actorContextService.isMetadataOnlyRole(this.role);
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private casesService: CasesService,
    private actorContextService: ActorContextService
  ) {}

  ngOnInit(): void {
    this.role = this.actorContextService.resolvePrimaryRole('REVIEWER');

    // 從 query params 還原 UI state（filter/tab 持久化）
    this.route.queryParams.subscribe(params => {
      this.uiState = {
        tab: params['tab'] ?? this._defaultTab(),
        status: params['status'] ?? this._defaultStatus(params['tab'] ?? this._defaultTab()),
        page: parseInt(params['page'] ?? '0', 10),
        size: parseInt(params['size'] ?? '20', 10)
      };
      this._loadCases();
    });
  }

  /** 依 role 設定 default tab */
  private _defaultTab(): string {
    switch (this.role) {
      case 'REVIEWER': return 'MY_QUEUE';
      case 'REREVIEWER': return 'MY_QUEUE';
      case 'HOSPITAL_MANAGER': return 'MY_MANAGED_PROJECTS';
      case 'SYSTEM_ADMIN': return 'ALL_CASES';
      default: return 'MY_QUEUE';
    }
  }

  private _defaultStatus(tab: string): string | undefined {
    switch (tab) {
      case 'MY_QUEUE':
        if (this.role === 'REREVIEWER') {
          return 'PENDING_REREVIEW';
        }
        return 'PENDING_INITIAL_REVIEW';
      case 'MY_MANAGED_PROJECTS':
        return 'SELECTED_PENDING_ASSIGNMENT';
      default:
        return undefined;
    }
  }

  /** Mode resolution: role + backend projectionType → modeLabel + command visibility */
  private _resolveMode(): void {
    if (this.projectionType === 'METADATA_ONLY') {
      this.showBusinessCommands = false;
      this.showProjectionNotice = true;
      this.modeLabel = '唯讀（metadata-only）';
    } else {
      this.showBusinessCommands = true;
      this.showProjectionNotice = false;
      this.modeLabel = this.actorContextService.isManagementRole(this.role)
        ? '管理者視角'
        : '審查佇列';
    }
  }

  private _loadCases(): void {
    const criteria: CasesQueryCriteria = {
      role: this.role,
      status: this.uiState.status,
      page: this.uiState.page,
      size: this.uiState.size
    };
    this.casesService.getCases(criteria).subscribe((result: CaseListResult) => {
      this.cases = result.items;
      this.projectionType = result.projectionType;
      this._resolveMode();
    });
  }

  /** tab 切換 → query params 持久化 */
  onTabChange(tab: string): void {
    const status = this._defaultStatus(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab, status },
      queryParamsHandling: 'merge'
    });
  }

  /** filter 變更 → query params 持久化 */
  onFilterChange(status: string): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { status, tab: this.uiState.tab },
      queryParamsHandling: 'merge'
    });
  }

  /** 導航至案件詳情 */
  navigateToDetail(caseId: string): void {
    this.router.navigate(['/cases', caseId]);
  }
}

