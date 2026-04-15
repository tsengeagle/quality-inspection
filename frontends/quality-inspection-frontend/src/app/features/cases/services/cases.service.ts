import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

/** 案件列表查詢條件 */
export interface CasesQueryCriteria {
  role?: string;
  status?: string;
  page?: number;
  size?: number;
  hospitalId?: string;
  batchId?: string;
}

/** 案件摘要（列表用） */
export interface CaseSummaryItem {
  caseId: string;
  patientId: string;
  status: string;
  stageLabel: string;
  assignee: string;
  createDate: string;
}

/** 案件詳情 */
export interface CaseDetailViewModel {
  caseId: string;
  caseTitle: string;
  patientId: string;
  status: string;
  stageLabel: string;
  role: string;
  projectionType: 'FULL' | 'METADATA_ONLY';
  isOwner: boolean;
  initialReviewSummary?: string;
  reviewerName?: string;
  rereviewerName?: string;
}

/** Revision 歷史條目 */
export interface RevisionHistoryItem {
  submissionId: string;
  version: number;
  submittedAt: string;
  authorName: string;
  actionType: string;
}

/** RevisionHistoryPage view model */
export interface RevisionHistoryViewModel {
  caseId: string;
  caseTitle: string;
  status: string;
  stageLabel: string;
  role: string;
  projectionType: 'FULL' | 'METADATA_ONLY';
  isOriginalRereviewer: boolean;
  hasBaseSubmission: boolean;
  revisions: RevisionHistoryItem[];
}

@Injectable({ providedIn: 'root' })
export class CasesService {

  /** GET /review-cases — stub，後續由 HttpClient 實作 */
  getCases(criteria: CasesQueryCriteria): Observable<CaseSummaryItem[]> {
    // TODO: replace with real HTTP call
    return of([]);
  }

  /** GET /review-cases/{caseId} — stub */
  getCaseDetail(caseId: string): Observable<CaseDetailViewModel | null> {
    // TODO: replace with real HTTP call
    return of(null);
  }

  /** GET /review-cases/{caseId}/revisions — stub */
  getRevisionHistory(caseId: string): Observable<RevisionHistoryViewModel | null> {
    // TODO: replace with real HTTP call
    return of(null);
  }
}
