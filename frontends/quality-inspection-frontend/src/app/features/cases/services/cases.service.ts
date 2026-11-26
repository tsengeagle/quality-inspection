import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, forkJoin, map, of, throwError } from 'rxjs';

import { environment } from '@env/environment';

type ProjectionType = 'FULL' | 'METADATA_ONLY';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

interface ApiErrorResponse {
  success: false;
  message?: string;
  errorCode?: string;
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

interface ReviewSubmissionRefDto {
  submissionId: string;
  revisionNo: number;
  status: string;
  submittedAt?: string | null;
}

interface AssignmentAssigneeSummaryDto {
  userId: string;
  displayName: string;
  displayLabel: string;
  assignedAt?: string | null;
}

interface AssignmentBootstrapDto {
  caseId: string;
  projectId: string;
  projectName: string;
  batchId: string;
  hospitalId: string;
  sourceCaseKey: string;
  medicalRecordType: string;
  status: string;
  pageMode: 'ASSIGN_INITIAL' | 'REASSIGN_INITIAL' | 'MIXED_ASSIGNMENT' | 'READONLY_BLOCKED';
  blocked: boolean;
  blockedReason?: string | null;
  allowedActions: string[];
  currentReviewer?: AssignmentAssigneeSummaryDto | null;
  currentRereviewer?: AssignmentAssigneeSummaryDto | null;
  reviewerDueAt?: string | null;
  rereviewerDueAt?: string | null;
  effectiveDueAt?: string | null;
}

interface AssignmentCandidateDto {
  userId: string;
  displayName: string;
  displayLabel: string;
  role: 'REVIEWER' | 'REREVIEWER';
  active: boolean;
  selectable: boolean;
  blockedReason?: string | null;
}

interface InitialReviewBootstrapDto {
  pageMode: 'NEW_INITIAL_REVIEW' | 'EDIT_DRAFT' | 'REVISION' | 'READONLY_BLOCKED';
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  readonly: boolean;
  blocked: boolean;
  blockedReason?: string | null;
  scoreSheetVersionId?: string | null;
  allowedActions: string[];
  currentSubmission?: ReviewSubmissionRefDto | null;
  editableSubmission?: ReviewSubmissionRefDto | null;
  basedOnVersion?: ReviewSubmissionRefDto | null;
}

interface RereviewBootstrapDto {
  pageMode:
    | 'NEW_REREVIEW'
    | 'EDIT_DRAFT'
    | 'REVISION'
    | 'READONLY_BLOCKED';
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  readonly: boolean;
  blocked: boolean;
  blockedReason?: string | null;
  scoreSheetVersionId?: string | null;
  allowedActions: string[];
  currentSubmission?: ReviewSubmissionRefDto | null;
  editableSubmission?: ReviewSubmissionRefDto | null;
  basedOnVersion?: ReviewSubmissionRefDto | null;
  initialSubmissionId?: string | null;
  initialSubmissionRevisionNo?: number | null;
  initialReviewerId?: string | null;
  initialReviewerName?: string | null;
  initialSubmittedAt?: string | null;
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
  initialReviewContext?: InitialReviewBootstrapDto | null;
  rereviewContext?: RereviewBootstrapDto | null;
}

interface RevisionHistoryResponseDto {
  caseId: string;
  submissions: SubmissionSummaryDto[];
}

interface SubmissionDetailAnswerDto {
  itemId: string;
  answerValue: string;
  derivedScore?: number | null;
}

interface SubmissionDetailDto {
  submissionId: string;
  caseId: string;
  stage: 'INITIAL' | 'REREVIEW';
  revisionNo: number;
  status: string;
  reviewerId: string;
  answers: SubmissionDetailAnswerDto[];
  comment?: string | null;
  attachmentRefsJson?: string | null;
  totalScore?: number | null;
  createdAt?: string | null;
  submittedAt?: string | null;
  maskedFields?: string[] | null;
}

interface StartInitialReviewResponseDto {
  submissionId: string;
}

interface SaveDraftResponseDto {
  submissionId: string;
  status: string;
  stage: string;
  totalScore?: number | null;
}

interface SubmitInitialReviewResponseDto {
  submissionId: string;
  status: string;
  caseId: string;
  caseStatus: string;
  totalScore?: number | null;
}

/** 不含 finalScore：回應複審醫師時不得回傳可反推初審分數的資訊（D-033、D-053）。 */
interface SubmitRereviewResponseDto {
  submissionId: string;
  status: string;
  caseId: string;
  caseStatus: string;
}

/** 複審結案的回應（D-053 第 2 點）。 */
/** D-054 第 4 點：固定初審的回應；不含品質線 */
interface FixInitialReviewResponseDto {
  caseId: string;
  caseStatus: string;
  submissionId: string;
  fixedAt: string;
}

interface CloseRereviewResponseDto {
  caseId: string;
  caseStatus: string;
  submissionId: string;
}

interface RevisionResultResponseDto {
  newSubmissionId: string;
  oldSubmissionId: string;
  revisionNo: number;
  totalScore?: number | null;
  status: string;
  stage: string;
  caseId: string;
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

export interface ReviewSubmissionRef {
  submissionId: string;
  version: number;
  status: string;
  submittedAt?: string;
}

export interface AssignmentAssigneeSummaryViewModel {
  userId: string;
  displayName: string;
  displayLabel: string;
  assignedAt?: string;
}

export interface AssignmentCandidateViewModel {
  userId: string;
  displayName: string;
  displayLabel: string;
  role: 'REVIEWER' | 'REREVIEWER';
  active: boolean;
  selectable: boolean;
  blockedReason?: string | null;
}

export interface AssignmentContextViewModel {
  caseId: string;
  projectId: string;
  projectName: string;
  batchId: string;
  hospitalId: string;
  sourceCaseKey: string;
  medicalRecordType: string;
  status: string;
  pageMode: 'ASSIGN_INITIAL' | 'REASSIGN_INITIAL' | 'MIXED_ASSIGNMENT' | 'READONLY_BLOCKED';
  blocked: boolean;
  blockedReason?: string | null;
  allowedActions: string[];
  currentReviewer?: AssignmentAssigneeSummaryViewModel;
  currentRereviewer?: AssignmentAssigneeSummaryViewModel;
  reviewerDueAt?: string;
  rereviewerDueAt?: string;
  effectiveDueAt?: string;
}

export interface AssignReviewerPayload {
  reviewerId: string;
  dueAt?: string | null;
}

export interface AssignRereviewerPayload {
  rereviewerId: string;
  dueAt?: string | null;
}

export interface InitialReviewBootstrapViewModel {
  pageMode: 'NEW_INITIAL_REVIEW' | 'EDIT_DRAFT' | 'REVISION' | 'READONLY_BLOCKED';
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  readonly: boolean;
  blocked: boolean;
  blockedReason?: string | null;
  scoreSheetVersionId?: string | null;
  allowedActions: string[];
  currentSubmission?: ReviewSubmissionRef;
  editableSubmission?: ReviewSubmissionRef;
  basedOnVersion?: ReviewSubmissionRef;
}

/**
 * 複審頁的 bootstrap。不含初審分數、finalScore：這些值會洩漏或可反推初審分數（D-033），
 * 後端即使回傳也不映射，避免留在前端快取。closedRevisionException 與 REVISION_EXCEPTION_CLOSED 已隨 D-032 撤銷。
 */
export interface RereviewBootstrapViewModel {
  pageMode:
    | 'NEW_REREVIEW'
    | 'EDIT_DRAFT'
    | 'REVISION'
    | 'READONLY_BLOCKED';
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  readonly: boolean;
  blocked: boolean;
  blockedReason?: string | null;
  scoreSheetVersionId?: string | null;
  allowedActions: string[];
  currentSubmission?: ReviewSubmissionRef;
  editableSubmission?: ReviewSubmissionRef;
  basedOnVersion?: ReviewSubmissionRef;
  initialSubmissionId?: string | null;
  initialSubmissionRevisionNo?: number | null;
  initialReviewerId?: string | null;
  initialReviewerName?: string | null;
  initialSubmittedAt?: string | null;
}

export interface CommandAnswerInput {
  itemId: string;
  answerValue: string;
}

export interface SubmissionDetailAnswer {
  itemId: string;
  answerValue: string;
  derivedScore?: number | null;
}

export interface SubmissionDetailViewModel {
  submissionId: string;
  caseId: string;
  stage: 'INITIAL' | 'REREVIEW';
  revisionNo: number;
  status: string;
  reviewerId: string;
  answers: SubmissionDetailAnswer[];
  comment?: string | null;
  attachmentRefsJson?: string | null;
  totalScore?: number | null;
  createdAt?: string | null;
  submittedAt?: string | null;
  maskedFields: string[];
}

export interface InitialReviewDraftPayload {
  answers: CommandAnswerInput[];
  comment?: string | null;
}

export interface RereviewDraftPayload {
  submissionId: string;
  answers?: CommandAnswerInput[];
  comment?: string | null;
  totalScore?: number | null;
}

export interface RereviewSubmitPayload {
  submissionId: string;
  answers?: CommandAnswerInput[];
  comment?: string | null;
}

export interface CommandResultViewModel {
  submissionId?: string;
  caseId?: string;
  caseStatus?: string;
  status?: string;
  stage?: string;
  totalScore?: number | null;
  finalScore?: number | null;
  revisionNo?: number;
  oldSubmissionId?: string;
  newSubmissionId?: string;
  /** 初審固定時間（UTC，D-054） */
  fixedAt?: string;
  /** 草稿內容沒有變更，後端未寫入（D-038 的 409 NO_CHANGES）；前端視為「已是最新」（D-044 第 5 點） */
  noChanges?: boolean;
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
  createdAt?: string | null;
  assignedAt?: string | null;
  dueAt?: string | null;
  closedAt?: string | null;
  cancelledAt?: string | null;
  submissions: SubmissionSummaryItem[];
  latestRereviewSubmissionId?: string | null;
  initialReviewContext?: InitialReviewBootstrapViewModel;
  rereviewContext?: RereviewBootstrapViewModel;
}

/** Revision 歷史條目 */
export type RevisionHistoryItem = SubmissionSummaryItem;

/** RevisionHistoryPage view model */
export interface RevisionHistoryViewModel {
  caseId: string;
  status: string;
  actorRole: string;
  projectionType: ProjectionType;
  revisions: RevisionHistoryItem[];
}

@Injectable({ providedIn: 'root' })
export class CasesService {

  private readonly baseUrl = `${environment.apiBasePath}/review-cases`;
  private readonly submissionBaseUrl = `${environment.apiBasePath}/review-submissions`;

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

  /** GET /review-cases/{caseId}/assignment-context */
  getAssignmentContext(caseId: string): Observable<AssignmentContextViewModel> {
    return this.http.get<ApiResponse<AssignmentBootstrapDto>>(`${this.baseUrl}/${caseId}/assignment-context`).pipe(
      map(response => this.mapAssignmentContext(response.data))
    );
  }

  /** GET /review-cases/{caseId}/assignment-candidates?role=... */
  getAssignmentCandidates(caseId: string, role: 'REVIEWER' | 'REREVIEWER', keyword?: string): Observable<AssignmentCandidateViewModel[]> {
    let params = new HttpParams().set('role', role);
    if (keyword?.trim()) {
      params = params.set('keyword', keyword.trim());
    }

    return this.http.get<ApiResponse<AssignmentCandidateDto[]>>(`${this.baseUrl}/${caseId}/assignment-candidates`, { params }).pipe(
      map(response => response.data.map(item => this.mapAssignmentCandidate(item)))
    );
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
        revisions: history.data.submissions.map(item => this.mapSubmission(item))
      }))
    );
  }

  getSubmissionDetail(submissionId: string): Observable<SubmissionDetailViewModel> {
    return this.http.get<ApiResponse<SubmissionDetailDto>>(`${this.submissionBaseUrl}/${submissionId}`).pipe(
      map(response => this.mapSubmissionDetail(response.data))
    );
  }

  startInitialReview(caseId: string): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<StartInitialReviewResponseDto>>(`${this.baseUrl}/${caseId}/initial-review/start`, {}).pipe(
      map(response => ({
        submissionId: response.data.submissionId
      }))
    );
  }

  saveInitialReviewDraft(caseId: string, payload: InitialReviewDraftPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<SaveDraftResponseDto>>(`${this.baseUrl}/${caseId}/initial-review/draft`, payload).pipe(
      map(response => this.mapSaveDraftResponse(response.data)),
      catchError(error => this.treatNoChangesAsUpToDate(error))
    );
  }

  submitInitialReview(caseId: string): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<SubmitInitialReviewResponseDto>>(`${this.baseUrl}/${caseId}/initial-review/submit`, {}).pipe(
      map(response => ({
        submissionId: response.data.submissionId,
        caseId: response.data.caseId,
        caseStatus: response.data.caseStatus,
        status: response.data.status,
        totalScore: response.data.totalScore
      }))
    );
  }

  /** D-054 第 1 點：固定目前有效的初審結果，不需要請求內容 */
  fixInitialReview(caseId: string, submissionId: string): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<FixInitialReviewResponseDto>>(
      `${this.baseUrl}/${caseId}/initial-review/submissions/${submissionId}/fix`,
      {}
    ).pipe(
      map(response => ({
        caseId: response.data.caseId,
        caseStatus: response.data.caseStatus,
        submissionId: response.data.submissionId,
        fixedAt: response.data.fixedAt
      }))
    );
  }

  reviseInitialReview(caseId: string, submissionId: string, payload: InitialReviewDraftPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<RevisionResultResponseDto>>(
      `${this.baseUrl}/${caseId}/initial-review/submissions/${submissionId}/revise`,
      payload
    ).pipe(
      map(response => this.mapRevisionResult(response.data))
    );
  }

  startRereview(caseId: string): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/${caseId}/start-rereview`, {}).pipe(
      map(() => ({ caseId }))
    );
  }

  saveRereviewDraft(caseId: string, payload: RereviewDraftPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<SaveDraftResponseDto>>(`${this.baseUrl}/${caseId}/rereview/draft`, payload).pipe(
      map(response => this.mapSaveDraftResponse(response.data)),
      catchError(error => this.treatNoChangesAsUpToDate(error))
    );
  }

  submitRereview(caseId: string, payload: RereviewSubmitPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<SubmitRereviewResponseDto>>(`${this.baseUrl}/${caseId}/rereview/submit`, payload).pipe(
      map(response => ({
        submissionId: response.data.submissionId,
        caseId: response.data.caseId,
        caseStatus: response.data.caseStatus,
        status: response.data.status
      }))
    );
  }

  /** 複審的獨立結案（D-034、D-053）。submissionId 是要結案的有效複審結果；不需要請求內容。 */
  closeRereview(caseId: string, submissionId: string): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<CloseRereviewResponseDto>>(
      `${this.baseUrl}/${caseId}/rereview/submissions/${submissionId}/close`,
      {}
    ).pipe(
      map(response => ({
        caseId: response.data.caseId,
        caseStatus: response.data.caseStatus,
        submissionId: response.data.submissionId
      }))
    );
  }

  reviseRereview(caseId: string, submissionId: string, payload: Pick<RereviewSubmitPayload, 'answers' | 'comment'>): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<RevisionResultResponseDto>>(
      `${this.baseUrl}/${caseId}/rereview/submissions/${submissionId}/revise`,
      payload
    ).pipe(
      map(response => this.mapRevisionResult(response.data))
    );
  }

  assignReviewer(caseId: string, payload: AssignReviewerPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/${caseId}/assign-reviewer`, payload).pipe(
      map(() => ({ caseId }))
    );
  }

  reassignReviewer(caseId: string, payload: AssignReviewerPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/${caseId}/reassign-reviewer`, payload).pipe(
      map(() => ({ caseId }))
    );
  }

  assignRereviewer(caseId: string, payload: AssignRereviewerPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/${caseId}/assign-rereviewer`, payload).pipe(
      map(() => ({ caseId }))
    );
  }

  reassignRereviewer(caseId: string, payload: AssignRereviewerPayload): Observable<CommandResultViewModel> {
    return this.http.post<ApiResponse<unknown>>(`${this.baseUrl}/${caseId}/reassign-rereviewer`, payload).pipe(
      map(() => ({ caseId }))
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
      createdAt: item.createdAt,
      assignedAt: item.assignedAt,
      dueAt: item.dueAt,
      closedAt: item.closedAt,
      cancelledAt: item.cancelledAt,
      submissions: item.submissions.map(submission => this.mapSubmission(submission)),
      latestRereviewSubmissionId: item.latestRereviewSubmissionId,
      initialReviewContext: item.initialReviewContext
        ? this.mapInitialReviewContext(item.initialReviewContext)
        : undefined,
      rereviewContext: item.rereviewContext
        ? this.mapRereviewContext(item.rereviewContext)
        : undefined
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

  private mapSubmissionDetail(item: SubmissionDetailDto): SubmissionDetailViewModel {
    return {
      submissionId: item.submissionId,
      caseId: item.caseId,
      stage: item.stage,
      revisionNo: item.revisionNo,
      status: item.status,
      reviewerId: item.reviewerId,
      answers: item.answers.map(answer => ({
        itemId: answer.itemId,
        answerValue: answer.answerValue,
        derivedScore: answer.derivedScore
      })),
      comment: item.comment,
      attachmentRefsJson: item.attachmentRefsJson,
      totalScore: item.totalScore,
      createdAt: item.createdAt,
      submittedAt: item.submittedAt,
      maskedFields: item.maskedFields ?? []
    };
  }

  /**
   * D-038：草稿沒有變更時，後端回 409 與錯誤代碼 NO_CHANGES，且沒有寫入。
   * D-044 第 5 點：前端視為「已是最新」，不顯示錯誤。其他錯誤照常拋出。
   */
  private treatNoChangesAsUpToDate(error: unknown): Observable<CommandResultViewModel> {
    if (error instanceof HttpErrorResponse && error.status === 409 && error.error?.error?.code === 'NO_CHANGES') {
      return of({ noChanges: true });
    }
    return throwError(() => error);
  }

  private mapSaveDraftResponse(item: SaveDraftResponseDto): CommandResultViewModel {
    return {
      submissionId: item.submissionId,
      status: item.status,
      stage: item.stage,
      totalScore: item.totalScore
    };
  }

  private mapRevisionResult(item: RevisionResultResponseDto): CommandResultViewModel {
    return {
      caseId: item.caseId,
      status: item.status,
      stage: item.stage,
      totalScore: item.totalScore,
      revisionNo: item.revisionNo,
      oldSubmissionId: item.oldSubmissionId,
      newSubmissionId: item.newSubmissionId,
      submissionId: item.newSubmissionId
    };
  }

  private mapInitialReviewContext(item: InitialReviewBootstrapDto): InitialReviewBootstrapViewModel {
    return {
      pageMode: item.pageMode,
      hasDraft: item.hasDraft,
      hasSubmittedBase: item.hasSubmittedBase,
      readonly: item.readonly,
      blocked: item.blocked,
      blockedReason: item.blockedReason,
      scoreSheetVersionId: item.scoreSheetVersionId,
      allowedActions: item.allowedActions,
      currentSubmission: this.mapSubmissionRef(item.currentSubmission),
      editableSubmission: this.mapSubmissionRef(item.editableSubmission),
      basedOnVersion: this.mapSubmissionRef(item.basedOnVersion)
    };
  }

  private mapRereviewContext(item: RereviewBootstrapDto): RereviewBootstrapViewModel {
    return {
      pageMode: item.pageMode,
      hasDraft: item.hasDraft,
      hasSubmittedBase: item.hasSubmittedBase,
      readonly: item.readonly,
      blocked: item.blocked,
      blockedReason: item.blockedReason,
      scoreSheetVersionId: item.scoreSheetVersionId,
      allowedActions: item.allowedActions,
      currentSubmission: this.mapSubmissionRef(item.currentSubmission),
      editableSubmission: this.mapSubmissionRef(item.editableSubmission),
      basedOnVersion: this.mapSubmissionRef(item.basedOnVersion),
      initialSubmissionId: item.initialSubmissionId,
      initialSubmissionRevisionNo: item.initialSubmissionRevisionNo,
      initialReviewerId: item.initialReviewerId,
      initialReviewerName: item.initialReviewerName,
      initialSubmittedAt: item.initialSubmittedAt
    };
  }

  private mapSubmissionRef(item?: ReviewSubmissionRefDto | null): ReviewSubmissionRef | undefined {
    if (!item) {
      return undefined;
    }

    return {
      submissionId: item.submissionId,
      version: item.revisionNo,
      status: item.status,
      submittedAt: item.submittedAt ?? undefined
    };
  }

  private mapAssignmentContext(item: AssignmentBootstrapDto): AssignmentContextViewModel {
    return {
      caseId: item.caseId,
      projectId: item.projectId,
      projectName: item.projectName,
      batchId: item.batchId,
      hospitalId: item.hospitalId,
      sourceCaseKey: item.sourceCaseKey,
      medicalRecordType: item.medicalRecordType,
      status: item.status,
      pageMode: item.pageMode,
      blocked: item.blocked,
      blockedReason: item.blockedReason,
      allowedActions: item.allowedActions,
      currentReviewer: this.mapAssignmentAssignee(item.currentReviewer),
      currentRereviewer: this.mapAssignmentAssignee(item.currentRereviewer),
      reviewerDueAt: item.reviewerDueAt ?? undefined,
      rereviewerDueAt: item.rereviewerDueAt ?? undefined,
      effectiveDueAt: item.effectiveDueAt ?? undefined
    };
  }

  private mapAssignmentAssignee(item?: AssignmentAssigneeSummaryDto | null): AssignmentAssigneeSummaryViewModel | undefined {
    if (!item) {
      return undefined;
    }

    return {
      userId: item.userId,
      displayName: item.displayName,
      displayLabel: item.displayLabel,
      assignedAt: item.assignedAt ?? undefined
    };
  }

  private mapAssignmentCandidate(item: AssignmentCandidateDto): AssignmentCandidateViewModel {
    return {
      userId: item.userId,
      displayName: item.displayName,
      displayLabel: item.displayLabel,
      role: item.role,
      active: item.active,
      selectable: item.selectable,
      blockedReason: item.blockedReason
    };
  }
}
