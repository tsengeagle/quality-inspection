import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SamplingService, SamplingBatchSummaryItem } from '../services/sampling.service';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CanonicalActorRole } from '@core/actor-context/actor-context.model';

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

  retryTargetBatchId: string | null = null;
  showRetryModal = false;

  get isMetadataOnly(): boolean {
    return this.actorContextService.isMetadataOnlyRole(this.role);
  }

  get availableTabs(): Array<{ value: BatchListTab; label: string }> {
    switch (this.role) {
      case 'HOSPITAL_MANAGER':
        return [
          { value: 'ALL_BATCHES', label: '全部批次' },
          { value: 'COMPLETED', label: '成功' },
          { value: 'FAILED', label: '失敗' },
          { value: 'RETRYABLE', label: '可重跑' }
        ];
      case 'AUDITOR':
        return [
          { value: 'ALL_BATCHES', label: '全部批次' },
          { value: 'COMPLETED', label: '成功' },
          { value: 'FAILED', label: '失敗' }
        ];
      case 'SYSTEM_ADMIN':
        return [
          { value: 'ALL_BATCHES', label: '全部批次' },
          { value: 'FAILED', label: '失敗' }
        ];
      default:
        return [];
    }
  }

  get pageSummary(): string {
    switch (this.role) {
      case 'HOSPITAL_MANAGER':
        return '本院抽選批次追蹤';
      case 'AUDITOR':
        return '授權範圍批次查詢';
      case 'SYSTEM_ADMIN':
        return '抽選批次 metadata 監控';
      default:
        return '抽選批次列表';
    }
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private samplingService: SamplingService,
    private actorContextService: ActorContextService
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
