import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

/** 抽選批次摘要（列表用） */
export interface SamplingBatchSummaryItem {
  batchId: string;
  batchName: string;
  status: string;
  createDate: string;
  projectId: string;
  projectName: string;
  executedBy: string;
  canRetry?: boolean;
}

/** 抽選批次詳情（執行頁用） */
export interface SamplingBatchDetailView {
  batchId: string;
  batchName: string;
  status: string;
  projectId: string;
  projectName: string;
  hospitalId: string;
  ruleSnapshot?: string;
  previewStats?: {
    totalEligible: number;
    totalSelected: number;
    selectionRate: number;
  };
  postExecuteResult?: {
    reason: string;
    details: string;
  };
}

/** 抽選批次查詢條件 */
export interface SamplingBatchQueryCriteria {
  role?: string;
  status?: string;
  page?: number;
  size?: number;
  hospitalId?: string;
}

@Injectable({ providedIn: 'root' })
export class SamplingService {

  getBatches(_criteria: SamplingBatchQueryCriteria): Observable<SamplingBatchSummaryItem[]> {
    return of([]);
  }

  getBatchDetail(_batchId: string): Observable<SamplingBatchDetailView | null> {
    return of(null);
  }
}
