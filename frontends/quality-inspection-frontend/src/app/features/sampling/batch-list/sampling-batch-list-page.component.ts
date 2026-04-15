import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SamplingService, SamplingBatchSummaryItem } from '../services/sampling.service';

/** SP-01 — SamplingBatchListPage (UI-007)
 *  Role-based tab matrix:
 *  ADMIN → MY_BATCHES / ALL_BATCHES
 *  MANAGER → MY_BATCHES / MY_PROJECTS
 *  SYSTEM_ADMIN → ALL_BATCHES
 *  REVIEWER → MY_BATCHES（唯讀） */
export type BatchListTab = 'MY_BATCHES' | 'ALL_BATCHES' | 'MY_PROJECTS';

export interface BatchListUIState {
  tab: BatchListTab;
  status: string;
  page: number;
  size: number;
}

@Component({
  selector: 'app-sampling-batch-list-page',
  templateUrl: './sampling-batch-list-page.component.html',
  styleUrls: ['./sampling-batch-list-page.component.scss']
})
export class SamplingBatchListPageComponent implements OnInit {

  /** 注入當前使用者 role（mock，待 shell 注入替換） */
  role = 'ADMIN';

  uiState: BatchListUIState = {
    tab: 'MY_BATCHES',
    status: '',
    page: 0,
    size: 20
  };

  batches: SamplingBatchSummaryItem[] = [];

  /** Role-based 可顯示 tabs */
  get availableTabs(): Array<{ value: BatchListTab; label: string }> {
    switch (this.role) {
      case 'MANAGER':    return [{ value: 'MY_BATCHES', label: '我的批次' }, { value: 'MY_PROJECTS', label: '我的專案' }];
      case 'SYSTEM_ADMIN': return [{ value: 'ALL_BATCHES', label: '所有批次' }];
      case 'REVIEWER':   return [{ value: 'MY_BATCHES', label: '我的批次' }];
      default:           return [{ value: 'MY_BATCHES', label: '我的批次' }, { value: 'ALL_BATCHES', label: '所有批次' }];
    }
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private samplingService: SamplingService
  ) {}

  ngOnInit(): void {
    const savedTab = this.route.snapshot.queryParamMap.get('tab');
    if (savedTab) { this.uiState.tab = savedTab as BatchListTab; }
    const savedStatus = this.route.snapshot.queryParamMap.get('status');
    if (savedStatus) { this.uiState.status = savedStatus; }

    this.uiState.tab = this._defaultTab();
    this._loadBatches();
  }

  private _defaultTab(): BatchListTab {
    if (this.role === 'SYSTEM_ADMIN') { return 'ALL_BATCHES'; }
    return 'MY_BATCHES';
  }

  private _loadBatches(): void {
    this.samplingService.getBatches({ role: this.role, status: this.uiState.status }).subscribe(data => {
      this.batches = data;
    });
  }

  onTabChange(tab: BatchListTab): void {
    this.uiState.tab = tab;
    this.router.navigate([], { relativeTo: this.route, queryParams: { tab }, queryParamsHandling: 'merge' });
    this._loadBatches();
  }

  onFilterChange(status: string): void {
    this.uiState.status = status;
    this.router.navigate([], { relativeTo: this.route, queryParams: { status }, queryParamsHandling: 'merge' });
    this._loadBatches();
  }

  navigateToExecute(batchId: string): void {
    this.router.navigate(['/sampling', batchId]);
  }
}

