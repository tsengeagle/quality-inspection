import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';

import { environment } from '@env/environment';

type ProjectionType = 'FULL' | 'METADATA_ONLY';

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

interface ReviewCaseSummaryDto {
  caseId: string;
  projectId: string;
  batchId: string;
  hospitalId: string;
  status: string;
  reviewerId?: string | null;
  rereviewerId?: string | null;
  medicalRecordType: string;
  sourceCaseKey: string;
  createdAt?: string | null;
  assignedAt?: string | null;
  dueAt?: string | null;
  projectionType?: ProjectionType | null;
}

interface SubmissionSummaryDto {
  submissionId: string;
  stage: 'INITIAL' | 'REREVIEW';
  reviewerId: string;
  status: string;
  revisionNo: number;
  totalScore?: number | null;
  comment?: string | null;
  submittedAt?: string | null;
  supersededBySubmissionId?: string | null;
}

interface ReviewCaseDetailDto {
  caseId: string;
  projectId: string;
  batchId: string;
  hospitalId: string;
  status: string;
  reviewerId?: string | null;
  rereviewerId?: string | null;
  medicalRecordType: string;
  sourceCaseKey: string;
  initialReviewScore?: number | null;
  finalScore?: number | null;
  finalConclusion?: string | null;
  createdAt?: string | null;
  assignedAt?: string | null;
  dueAt?: string | null;
  closedAt?: string | null;
  cancelledAt?: string | null;
  submissions: SubmissionSummaryDto[];
  latestRereviewSubmissionId?: string | null;
  projectionType: ProjectionType;
  actorRole: string;
  isOwner: boolean;
}

interface RevisionHistoryResponseDto {
  caseId: string;
  submissions: SubmissionSummaryDto[];
  hasExceptionEntry: boolean;
}

/** 案件列表查詢條件 */
export interface CasesQueryCriteria {
  role?: string;
  status?: string;
  page?: number;
  size?: number;
  hospitalId?: string;
  batchId?: string;
  projectId?: string;
}

/** 案件摘要（列表用） */
export interface CaseSummaryItem {
  caseId: string;
  status: string;
  projectionType: ProjectionType;
  batchId: string;
  hospitalId: string;
  medicalRecordType: string;
  sourceCaseKey: string;
  reviewerId?: string | null;
  rereviewerId?: string | null;
  createdAt?: string | null;
  assignedAt?: string | null;
  dueAt?: string | null;
}

export interface CaseListResult {
  items: CaseSummaryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  projectionType?: ProjectionType;
}

export interface SubmissionSummaryItem {
  submissionId: string;
  stage: 'INITIAL' | 'REREVIEW';
  reviewerId: string;
  status: string;
  revisionNo: number;
  totalScore?: number | null;
  comment?: string | null;
  submittedAt?: string | null;
  supersededBySubmissionId?: string | null;
  isCurrentEffective: boolean;
}

/** 案件詳情 */
export interface CaseDetailViewModel {
  caseId: string;
  status: string;
  projectionType: ProjectionType;
  actorRole: string;
  isOwner: boolean;
  batchId: string;
  hospitalId: string;
  reviewerId?: string | null;
  rereviewerId?: string | null;
  medicalRecordType: string;
  sourceCaseKey: string;
  initialReviewScore?: number | null;
  finalScore?: number | null;
  finalConclusion?: string | null;
  createdAt?: string | null;
  assignedAt?: string | null;
  dueAt?: string | null;
  closedAt?: string | null;
  cancelledAt?: string | null;
  submissions: SubmissionSummaryItem[];
  latestRereviewSubmissionId?: string | null;
}

/** Revision 歷史條目 */
export type RevisionHistoryItem = SubmissionSummaryItem;

/** RevisionHistoryPage view model */
export interface RevisionHistoryViewModel {
  caseId: string;
  status: string;
  actorRole: string;
  projectionType: ProjectionType;
  hasExceptionEntry: boolean;
  revisions: RevisionHistoryItem[];
}

@Injectable({ providedIn: 'root' })
export class CasesService {

  private readonly baseUrl = `${environment.apiBasePath}/review-cases`;

  constructor(private readonly http: HttpClient) {}

  /** GET /review-cases */
  getCases(criteria: CasesQueryCriteria): Observable<CaseListResult> {
    let params = new HttpParams();

    if (criteria.status) {
      params = params.set('status', criteria.status);
    }
    if (criteria.page != null) {
      params = params.set('page', String(criteria.page));
    }
    if (criteria.size != null) {
      params = params.set('size', String(criteria.size));
    }
    if (criteria.hospitalId) {
      params = params.set('hospitalId', criteria.hospitalId);
    }
    if (criteria.batchId) {
      params = params.set('batchId', criteria.batchId);
    }
    if (criteria.projectId) {
      params = params.set('projectId', criteria.projectId);
    }

    return this.http.get<ApiResponse<PageResponse<ReviewCaseSummaryDto>>>(this.baseUrl, { params }).pipe(
      map(response => ({
        items: response.data.content.map(item => this.mapCaseSummary(item)),
        page: response.data.page,
        size: response.data.size,
        totalElements: response.data.totalElements,
        totalPages: response.data.totalPages,
        projectionType: response.data.content[0]?.projectionType ?? undefined
      }))
    );
  }

  /** GET /review-cases/{caseId} */
  getCaseDetail(caseId: string): Observable<CaseDetailViewModel> {
    return this.fetchCaseDetail(caseId);
  }

  /** GET /review-cases/{caseId}/revision-history + /review-cases/{caseId} */
  getRevisionHistory(caseId: string): Observable<RevisionHistoryViewModel> {
    return forkJoin({
      detail: this.fetchCaseDetail(caseId),
      history: this.http.get<ApiResponse<RevisionHistoryResponseDto>>(`${this.baseUrl}/${caseId}/revision-history`)
    }).pipe(
      map(({ detail, history }) => ({
        caseId: history.data.caseId,
        status: detail.status,
        actorRole: detail.actorRole,
        projectionType: detail.projectionType,
        hasExceptionEntry: history.data.hasExceptionEntry,
        revisions: history.data.submissions.map(item => this.mapSubmission(item))
      }))
    );
  }

  private fetchCaseDetail(caseId: string): Observable<CaseDetailViewModel> {
    return this.http.get<ApiResponse<ReviewCaseDetailDto>>(`${this.baseUrl}/${caseId}`).pipe(
      map(response => this.mapCaseDetail(response.data))
    );
  }

  private mapCaseSummary(item: ReviewCaseSummaryDto): CaseSummaryItem {
    return {
      caseId: item.caseId,
      status: item.status,
      projectionType: item.projectionType ?? 'FULL',
      batchId: item.batchId,
      hospitalId: item.hospitalId,
      medicalRecordType: item.medicalRecordType,
      sourceCaseKey: item.sourceCaseKey,
      reviewerId: item.reviewerId,
      rereviewerId: item.rereviewerId,
      createdAt: item.createdAt,
      assignedAt: item.assignedAt,
      dueAt: item.dueAt
    };
  }

  private mapCaseDetail(item: ReviewCaseDetailDto): CaseDetailViewModel {
    return {
      caseId: item.caseId,
      status: item.status,
      projectionType: item.projectionType,
      actorRole: item.actorRole,
      isOwner: item.isOwner,
      batchId: item.batchId,
      hospitalId: item.hospitalId,
      reviewerId: item.reviewerId,
      rereviewerId: item.rereviewerId,
      medicalRecordType: item.medicalRecordType,
      sourceCaseKey: item.sourceCaseKey,
      initialReviewScore: item.initialReviewScore,
      finalScore: item.finalScore,
      finalConclusion: item.finalConclusion,
      createdAt: item.createdAt,
      assignedAt: item.assignedAt,
      dueAt: item.dueAt,
      closedAt: item.closedAt,
      cancelledAt: item.cancelledAt,
      submissions: item.submissions.map(submission => this.mapSubmission(submission)),
      latestRereviewSubmissionId: item.latestRereviewSubmissionId
    };
  }

  private mapSubmission(item: SubmissionSummaryDto): SubmissionSummaryItem {
    return {
      submissionId: item.submissionId,
      stage: item.stage,
      reviewerId: item.reviewerId,
      status: item.status,
      revisionNo: item.revisionNo,
      totalScore: item.totalScore,
      comment: item.comment,
      submittedAt: item.submittedAt,
      supersededBySubmissionId: item.supersededBySubmissionId,
      isCurrentEffective: item.status === 'SUBMITTED' && !item.supersededBySubmissionId
    };
  }
}
