import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SamplingService, SamplingBatchSummaryItem } from '../services/sampling.service';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CanonicalActorRole } from '@core/actor-context/actor-context.model';
import { AccessControlService } from '@shared/access/access-control.service';

export type SamplingBatchListRole = CanonicalActorRole;

export type BatchListTab = 'ALL_BATCHES' | 'COMPLETED' | 'FAILED' | 'RETRYABLE';

export interface BatchListUIState {
  tab: BatchListTab;
  status: '' | 'COMPLETED' | 'FAILED' | 'RETRYABLE';
  page: number;
  size: number;
}

export interface BatchFilterState {
  projectId: string;
  batchId: string;
}

@Component({
  selector: 'app-sampling-batch-list-page',
  templateUrl: './sampling-batch-list-page.component.html',
  styleUrls: ['./sampling-batch-list-page.component.scss']
})
export class SamplingBatchListPageComponent implements OnInit {

  role: SamplingBatchListRole = 'HOSPITAL_MANAGER';

  uiState: BatchListUIState = {
    tab: 'ALL_BATCHES',
    status: '',
    page: 0,
    size: 20
  };

  filters: BatchFilterState = {
    projectId: '',
    batchId: ''
  };

  batches: SamplingBatchSummaryItem[] = [];
  totalElements = 0;
  totalPages = 0;

  retryTargetBatchId: string | null = null;
  showRetryModal = false;

  get totalCount(): number {
    return this.totalElements;
  }

  get failedCount(): number {
    return this.batches.filter(batch => batch.status === 'FAILED').length;
  }

  get retryableCount(): number {
    return this.batches.filter(batch => batch.status === 'FAILED' && batch.canRetry).length;
  }

  get activeFilterChips(): string[] {
    const chips: string[] = [];

    if (this.uiState.tab !== 'ALL_BATCHES') {
      chips.push(`狀態：${this.availableTabs.find(tab => tab.value === this.uiState.tab)?.label ?? this.uiState.tab}`);
    }
    if (this.filters.projectId) {
      chips.push(`專案：${this.filters.projectId}`);
    }
    if (this.filters.batchId) {
      chips.push(`批次：${this.filters.batchId}`);
    }

    return chips;
  }

  get isMetadataOnly(): boolean {
    return this.accessControlService.getProjectionType() === 'METADATA_ONLY';
  }

  get availableTabs(): Array<{ value: BatchListTab; label: string }> {
    const tabs: Array<{ value: BatchListTab; label: string }> = [
      { value: 'ALL_BATCHES', label: '全部批次' }
    ];

    if (!this.isMetadataOnly) {
      tabs.push({ value: 'COMPLETED', label: '成功' });
    }

    tabs.push({ value: 'FAILED', label: '失敗' });

    if (this.accessControlService.hasAnyPermission('RETRY_SAMPLING_BATCH')) {
      tabs.push({ value: 'RETRYABLE', label: '可重跑' });
    }

    return tabs;
  }

  get pageSummary(): string {
    if (this.accessControlService.hasAnyPermission('RETRY_SAMPLING_BATCH')) {
      return '本院抽選批次追蹤';
    }

    if (this.isMetadataOnly) {
      return '抽選批次 metadata 監控';
    }

    if (this.accessControlService.hasAnyPermission('VIEW_SAMPLING_BATCH_CASES')) {
      return '授權範圍批次查詢';
    }

    return '抽選批次列表';
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private samplingService: SamplingService,
    private actorContextService: ActorContextService,
    private accessControlService: AccessControlService
  ) {}

  ngOnInit(): void {
    this.role = this.actorContextService.resolvePrimaryRole('HOSPITAL_MANAGER');

    this.route.queryParams.subscribe(params => {
      this.uiState.tab = (params['tab'] as BatchListTab) ?? this._defaultTab();
      this.uiState.status = this._tabToStatus(this.uiState.tab);
      this.filters = {
        projectId: params['projectId'] ?? '',
        batchId: params['batchId'] ?? ''
      };
      this._loadBatches();
    });
  }

  private _defaultTab(): BatchListTab {
    return 'ALL_BATCHES';
  }

  private _loadBatches(): void {
    this.samplingService.getBatches({
      role: this.role,
      status: this.uiState.status,
      projectId: this.filters.projectId || undefined,
      batchId: this.filters.batchId || undefined,
      page: this.uiState.page,
      size: this.uiState.size
    }).subscribe(result => {
      this.batches = result.items;
      this.totalElements = result.totalElements;
      this.totalPages = result.totalPages;
    });
  }

  private _tabToStatus(tab: BatchListTab): BatchListUIState['status'] {
    switch (tab) {
      case 'COMPLETED':
        return 'COMPLETED';
      case 'FAILED':
        return 'FAILED';
      case 'RETRYABLE':
        return 'RETRYABLE';
      default:
        return '';
    }
  }

  onTabChange(tab: string): void {
    const nextTab = tab as BatchListTab;
    this.uiState.tab = nextTab;
    this.uiState.status = this._tabToStatus(nextTab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: nextTab },
      queryParamsHandling: 'merge'
    });
  }

  onFilterChange(filters: Partial<BatchFilterState>): void {
    this.filters = {
      ...this.filters,
      ...filters
    };

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: this.filters,
      queryParamsHandling: 'merge'
    });
  }

  onViewCases(batchId: string): void {
    this.router.navigate(['/cases'], {
      queryParams: { batchId }
    });
  }

  openRetryConfirm(batchId: string): void {
    this.retryTargetBatchId = batchId;
    this.showRetryModal = true;
  }

  cancelRetry(): void {
    this.showRetryModal = false;
    this.retryTargetBatchId = null;
  }

  confirmRetry(batchId: string): void {
    this.cancelRetry();
    // F-001: 呼叫真實 POST /sampling-batches/{batchId}/retry
    this.samplingService.retrySamplingBatch(batchId).subscribe({
      next: () => {
        this._loadBatches();
      }
    });
  }
}
