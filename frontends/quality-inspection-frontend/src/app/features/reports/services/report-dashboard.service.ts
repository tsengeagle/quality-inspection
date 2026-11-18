import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '@env/environment';
import { ExportSummary } from './report-export.service';

/** 與匯出的 summary 同一個後端型別（DashboardSummary）。 */
export type DashboardSummary = ExportSummary;

export interface DashboardCriteria {
  /** UTC ISO-8601，含起；與 to 同時有值或同時沒有（不限期間）。 */
  from?: string;
  /** UTC ISO-8601，不含迄。 */
  to?: string;
  projectId?: string;
  /** 只有持有 SYSTEM_ADMIN 的帳號會帶；空白視為全部院區。 */
  hospitalId?: string;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class ReportDashboardService {

  private readonly summaryUrl = `${environment.apiBasePath}/reports/dashboard-summary`;

  constructor(private readonly http: HttpClient) {}

  /** GET /reports/dashboard-summary — api-spec 15.1。沒有值的條件不送。 */
  fetchSummary(criteria: DashboardCriteria): Observable<DashboardSummary> {
    let params = new HttpParams();
    if (criteria.from && criteria.to) {
      params = params.set('from', criteria.from).set('to', criteria.to);
    }
    const projectId = criteria.projectId?.trim();
    if (projectId) {
      params = params.set('projectId', projectId);
    }
    const hospitalId = criteria.hospitalId?.trim();
    if (hospitalId) {
      params = params.set('hospitalId', hospitalId);
    }

    return this.http
      .get<ApiResponse<DashboardSummary>>(this.summaryUrl, { params })
      .pipe(map(res => res.data));
  }
}
