import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { EMPTY, Observable, expand, map, reduce, tap } from 'rxjs';

import { environment } from '@env/environment';

/** UI-017 §5.1：下載時每頁筆數（API 上限）。 */
export const EXPORT_DOWNLOAD_PAGE_SIZE = 1000;

/** 目前 API 只支援的報表類型（api-spec 15.2）。 */
export const REVIEW_COMPLETION = 'REVIEW_COMPLETION';

// ── BE response shapes (private) ──────────────────────────────────────────
interface ApiResponse<T> {
  success: boolean;
  data: T;
}

interface ExportRowDto {
  caseId: string;
  projectId: string;
  hospitalId: string;
  status: string;
  reviewerId: string | null;
  rereviewerId: string | null;
  initialScore: number | null;
  finalScore: number | null;
  createdAt: string | null;
  closedAt: string | null;
  maskedFields?: string[] | null;
}

interface ExportResultDto {
  reportType: string;
  rows: ExportRowDto[];
  summary: ExportSummary;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ── Public view models ─────────────────────────────────────────────────────

export interface ExportSummary {
  totalCases: number;
  closedCases: number;
  rereviewedCases: number;
  overdueCases: number;
  /** 0～1 的比例。 */
  completionRate: number;
  rereviewRate: number;
  overdueRate: number;
}

export interface ExportRowView {
  caseId: string;
  projectId: string;
  hospitalId: string;
  status: string;
  reviewerId: string | null;
  rereviewerId: string | null;
  initialScore: number | null;
  finalScore: number | null;
  /** UTC 時間點（後端 Instant），原樣保留 API 字串。 */
  createdAt: string | null;
  /** UTC 時間點（後端 Instant，D-075 第 6 點），原樣保留 API 字串。 */
  closedAt: string | null;
  /** 後端遮罩為 null 的欄位名稱；前端只依此判斷，不自行推論（UI-017 §2）。 */
  maskedFields: string[];
}

export interface ExportPageResult {
  reportType: string;
  rows: ExportRowView[];
  summary: ExportSummary;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ExportCriteria {
  /** UTC ISO-8601，含起。 */
  from: string;
  /** UTC ISO-8601，不含迄。 */
  to: string;
  /** 只有 SYSTEM_ADMIN 會帶；空白視為全部院區。 */
  hospitalId?: string;
}

export interface ExportPageRequest extends ExportCriteria {
  page: number;
  size: number;
}

export type ExportDownloadErrorReason = 'DATA_CHANGED';

/** 下載期間資料有變動（UI-017 §5.1）。 */
export class ExportDownloadError extends Error {
  constructor(readonly reason: ExportDownloadErrorReason) {
    super(`Export download aborted: ${reason}`);
    this.name = 'ExportDownloadError';
  }
}

@Injectable({ providedIn: 'root' })
export class ReportExportService {

  private readonly exportUrl = `${environment.apiBasePath}/reports/export`;

  constructor(private readonly http: HttpClient) {}

  /** POST /reports/export — api-spec 15.2 */
  fetchPage(request: ExportPageRequest): Observable<ExportPageResult> {
    const body: Record<string, unknown> = {
      reportType: REVIEW_COMPLETION,
      from: request.from,
      to: request.to,
      page: request.page,
      size: request.size
    };
    const hospitalId = request.hospitalId?.trim();
    if (hospitalId) {
      body['hospitalId'] = hospitalId;
    }

    return this.http
      .post<ApiResponse<ExportResultDto>>(this.exportUrl, body)
      .pipe(map(res => this.mapResult(res.data)));
  }

  /**
   * 逐頁取完整個區間（UI-017 §5.1）。
   * 依序請求；任一頁 totalElements 與第一頁不同即以 DATA_CHANGED 中止，任一頁失敗即中止。
   * 只有全部成功才會發出一次完整的資料列。
   */
  fetchAllRows(criteria: ExportCriteria, onProgress?: (done: number, total: number) => void): Observable<ExportRowView[]> {
    const fetch = (page: number) =>
      this.fetchPage({ ...criteria, page, size: EXPORT_DOWNLOAD_PAGE_SIZE });

    let expectedTotal: number | null = null;

    return fetch(0).pipe(
      expand(result => {
        const next = result.page + 1;
        return next < result.totalPages ? fetch(next) : EMPTY;
      }, 1),
      tap(result => {
        if (expectedTotal === null) {
          expectedTotal = result.totalElements;
        } else if (result.totalElements !== expectedTotal) {
          throw new ExportDownloadError('DATA_CHANGED');
        }
        onProgress?.(result.page + 1, Math.max(result.totalPages, 1));
      }),
      reduce((rows, result) => rows.concat(result.rows), [] as ExportRowView[])
    );
  }

  private mapResult(dto: ExportResultDto): ExportPageResult {
    return {
      reportType: dto.reportType,
      rows: (dto.rows ?? []).map(row => ({
        caseId: row.caseId,
        projectId: row.projectId,
        hospitalId: row.hospitalId,
        status: row.status,
        reviewerId: row.reviewerId,
        rereviewerId: row.rereviewerId,
        initialScore: row.initialScore,
        finalScore: row.finalScore,
        createdAt: row.createdAt,
        closedAt: row.closedAt,
        maskedFields: row.maskedFields ?? []
      })),
      summary: dto.summary,
      page: dto.page,
      size: dto.size,
      totalElements: dto.totalElements,
      totalPages: dto.totalPages
    };
  }
}
