import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

/** 抽選批次摘要（列表用） */
export interface SamplingBatchSummaryItem {
  batchId: string;
  batchName: string;
  status: string;
  createdAt: string;
  projectId: string;
  projectName: string;
  retryOfBatchId?: string | null;
  selectedCount?: number | null;
  canRetry: boolean;
}

/** 抽選執行頁上下文（UI-006） */
export interface SamplingExecuteContext {
  projectId: string;
  projectName: string;
  hospitalId: string;
  ruleSummary: string;
  previewReady: boolean;
  executing: boolean;
  executeFailed: boolean;
  executedBatchId: string | null;
  blockedReason: string | null;
  previewStats: {
    populationCount: number;
    eligibleCount: number;
    excludedCount: number;
  } | null;
  postExecuteSummary: {
    tone: 'success' | 'failed';
    title: string;
    description: string;
    batchId: string | null;
  } | null;
}

/** 抽選批次查詢條件 */
export interface SamplingBatchQueryCriteria {
  role?: string;
  status?: string;
  page?: number;
  size?: number;
  hospitalId?: string;
  projectId?: string;
  batchId?: string;
}

const BATCH_FIXTURES: SamplingBatchSummaryItem[] = [
  {
    batchId: 'BATCH-FAILED-001',
    batchName: '2026-Q2 抽選失敗批次',
    status: 'FAILED',
    createdAt: '2026-04-14T09:20:00+08:00',
    projectId: 'PROJECT-001',
    projectName: '2026 年第二季品質審查',
    retryOfBatchId: null,
    selectedCount: null,
    canRetry: true
  },
  {
    batchId: 'BATCH-COMPLETED-001',
    batchName: '2026-Q2 抽選成功批次',
    status: 'COMPLETED',
    createdAt: '2026-04-13T11:00:00+08:00',
    projectId: 'PROJECT-001',
    projectName: '2026 年第二季品質審查',
    retryOfBatchId: null,
    selectedCount: 24,
    canRetry: false
  }
];

@Injectable({ providedIn: 'root' })
export class SamplingService {

  getBatches(criteria: SamplingBatchQueryCriteria): Observable<SamplingBatchSummaryItem[]> {
    const filtered = BATCH_FIXTURES.filter(batch => {
      if (criteria.status === 'COMPLETED' && batch.status !== 'COMPLETED') {
        return false;
      }

      if (criteria.status === 'FAILED' && batch.status !== 'FAILED') {
        return false;
      }

      if (criteria.status === 'RETRYABLE' && !batch.canRetry) {
        return false;
      }

      if (criteria.projectId && batch.projectId !== criteria.projectId) {
        return false;
      }

      if (criteria.batchId && batch.batchId !== criteria.batchId) {
        return false;
      }

      return true;
    });

    return of(filtered);
  }

  getExecuteContext(projectId: string): Observable<SamplingExecuteContext> {
    return of({
      projectId,
      projectName: '2026 年第二季品質審查',
      hospitalId: 'HOSP-01',
      ruleSummary: '抽樣方式：STRATIFIED\n分層依據：院區 + 病歷類型\n排除規則：急診與門診短案排除',
      previewReady: false,
      executing: false,
      executeFailed: false,
      executedBatchId: null,
      blockedReason: null,
      previewStats: null,
      postExecuteSummary: null
    });
  }
}
