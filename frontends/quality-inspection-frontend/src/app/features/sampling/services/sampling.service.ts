import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, of } from 'rxjs';
import { environment } from '@env/environment';

// ── BE response shapes (private) ──────────────────────────────────────────
interface ApiResponse<T> {
  success: boolean;
  data: T;
}

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface SamplingBatchSummaryDto {
  batchId: string;
  projectId: string;
  executionMode: string;
  status: string;
  populationCount: number;
  eligibleCount: number;
  selectedCount: number | null;
  retryOfBatchId: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

// ── Public view-model interfaces ──────────────────────────────────────────

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

/** 抽選批次列表結果（分頁） */
export interface SamplingBatchListResult {
  items: SamplingBatchSummaryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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

@Injectable({ providedIn: 'root' })
export class SamplingService {

  private readonly batchBaseUrl = `${environment.apiBasePath}/sampling-batches`;
  private readonly projectBaseUrl = `${environment.apiBasePath}/review-projects`;

  constructor(private readonly http: HttpClient) {}

  /** GET /sampling-batches — B-004 */
  getBatches(criteria: SamplingBatchQueryCriteria): Observable<SamplingBatchListResult> {
    let params = new HttpParams();

    // RETRYABLE 是 FE-only 概念，對應 BE status=FAILED
    const backendStatus = criteria.status === 'RETRYABLE' ? 'FAILED' : criteria.status;
    if (backendStatus) {
      params = params.set('status', backendStatus);
    }
    if (criteria.projectId) {
      params = params.set('projectId', criteria.projectId);
    }
    if (criteria.page != null) {
      params = params.set('page', String(criteria.page));
    }
    if (criteria.size != null) {
      params = params.set('size', String(criteria.size));
    }

    return this.http
      .get<ApiResponse<PageResponse<SamplingBatchSummaryDto>>>(this.batchBaseUrl, { params })
      .pipe(
        map(res => ({
          items: res.data.content.map(dto => this.mapBatchSummary(dto)),
          page: res.data.page,
          size: res.data.size,
          totalElements: res.data.totalElements,
          totalPages: res.data.totalPages
        }))
      );
  }

  /** POST /review-projects/{projectId}/sampling/preview — B-002 */
  previewSampling(projectId: string): Observable<{ populationCount: number; eligibleCount: number; excludedCount: number }> {
    return this.http
      .post<ApiResponse<{ populationCount: number; eligibleCount: number; excludedCount: number }>>(
        `${this.projectBaseUrl}/${projectId}/sampling/preview`,
        {}
      )
      .pipe(map(res => res.data));
  }

  /** POST /review-projects/{projectId}/sampling/execute — B-003 */
  executeSampling(projectId: string): Observable<{ batchId: string }> {
    return this.http
      .post<ApiResponse<{ batchId: string }>>(
        `${this.projectBaseUrl}/${projectId}/sampling/execute`,
        {}
      )
      .pipe(map(res => res.data));
  }

  /** POST /sampling-batches/{batchId}/retry — F-001 */
  retrySamplingBatch(batchId: string): Observable<{ batchId: string }> {
    return this.http
      .post<ApiResponse<{ batchId: string }>>(
        `${this.batchBaseUrl}/${batchId}/retry`,
        {}
      )
      .pipe(map(res => res.data));
  }

  // TODO [B-001]: 無對應 BE 專屬端點。
  // projectName/hospitalId 可由 GET /review-projects/{projectId} 衍生；
  // blockedReason(COMPLETED_BATCH_EXISTS) 需額外查詢或新端點。
  // 待架構決策後（A: 新增 BE 端點 / B: FE 組合查詢）補齊。
  getExecuteContext(projectId: string): Observable<SamplingExecuteContext> {
    return of({
      projectId,
      projectName: '',
      hospitalId: '',
      ruleSummary: '',
      previewReady: false,
      executing: false,
      executeFailed: false,
      executedBatchId: null,
      blockedReason: null,
      previewStats: null,
      postExecuteSummary: null
    });
  }

  private mapBatchSummary(dto: SamplingBatchSummaryDto): SamplingBatchSummaryItem {
    return {
      batchId: dto.batchId,
      batchName: dto.batchId,           // BE 無 batchName，暫以 batchId 代替
      status: dto.status,
      createdAt: dto.startedAt ?? '',   // BE startedAt → FE createdAt
      projectId: dto.projectId,
      projectName: '',                   // SamplingBatchSummary 無 projectName
      retryOfBatchId: dto.retryOfBatchId,
      selectedCount: dto.selectedCount,
      canRetry: dto.status === 'FAILED'
    };
  }
}
