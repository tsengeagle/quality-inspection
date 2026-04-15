import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '@env/environment';

import { CasesService } from './cases.service';

describe('CasesService', () => {
  let service: CasesService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CasesService]
    });

    service = TestBed.inject(CasesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('maps review case list query truth from backend response', () => {
    let actual: unknown;

    service.getCases({ status: 'PENDING_INITIAL_REVIEW', page: 0, size: 20 }).subscribe(result => {
      actual = result;
    });

    const request = httpMock.expectOne(req => req.url === `${environment.apiBasePath}/review-cases`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('status')).toBe('PENDING_INITIAL_REVIEW');
    request.flush({
      success: true,
      data: {
        content: [
          {
            caseId: 'CASE-009',
            projectId: 'PROJECT-001',
            batchId: 'BATCH-001',
            hospitalId: 'H001',
            status: 'PENDING_INITIAL_REVIEW',
            reviewerId: 'reviewer-1',
            rereviewerId: 'rereviewer-1',
            medicalRecordType: 'OUTPATIENT',
            sourceCaseKey: 'SK-001',
            createdAt: '2026-04-01T08:00:00',
            assignedAt: '2026-04-01T09:00:00',
            dueAt: '2026-04-03T23:59:00',
            projectionType: 'FULL'
          }
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1
      }
    });

    expect(actual).toEqual({
      items: [
        {
          caseId: 'CASE-009',
          status: 'PENDING_INITIAL_REVIEW',
          projectionType: 'FULL',
          batchId: 'BATCH-001',
          hospitalId: 'H001',
          medicalRecordType: 'OUTPATIENT',
          sourceCaseKey: 'SK-001',
          reviewerId: 'reviewer-1',
          rereviewerId: 'rereviewer-1',
          createdAt: '2026-04-01T08:00:00',
          assignedAt: '2026-04-01T09:00:00',
          dueAt: '2026-04-03T23:59:00'
        }
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      projectionType: 'FULL'
    });
  });

  it('merges case detail truth and revision history truth for UI-014', () => {
    let actual: unknown;

    service.getRevisionHistory('CASE-014').subscribe(result => {
      actual = result;
    });

    const detailRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-014`);
    expect(detailRequest.request.method).toBe('GET');
    detailRequest.flush({
      success: true,
      data: {
        caseId: 'CASE-014',
        projectId: 'PROJECT-001',
        batchId: 'BATCH-001',
        hospitalId: 'H001',
        status: 'CLOSED',
        reviewerId: 'reviewer-1',
        rereviewerId: 'rereviewer-1',
        medicalRecordType: 'OUTPATIENT',
        sourceCaseKey: 'SK-014',
        submissions: [],
        latestRereviewSubmissionId: 'SUB-002',
        projectionType: 'FULL',
        actorRole: 'REREVIEWER',
        isOwner: true
      }
    });

    const historyRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-014/revision-history`);
    expect(historyRequest.request.method).toBe('GET');
    historyRequest.flush({
      success: true,
      data: {
        caseId: 'CASE-014',
        hasExceptionEntry: true,
        submissions: [
          {
            submissionId: 'SUB-001',
            stage: 'INITIAL',
            reviewerId: 'reviewer-1',
            status: 'SUPERSEDED',
            revisionNo: 1,
            submittedAt: '2026-04-01T08:00:00',
            supersededBySubmissionId: 'SUB-002'
          },
          {
            submissionId: 'SUB-002',
            stage: 'REREVIEW',
            reviewerId: 'rereviewer-1',
            status: 'SUBMITTED',
            revisionNo: 2,
            submittedAt: '2026-04-02T08:00:00',
            supersededBySubmissionId: null
          }
        ]
      }
    });

    expect(actual).toEqual({
      caseId: 'CASE-014',
      status: 'CLOSED',
      actorRole: 'REREVIEWER',
      projectionType: 'FULL',
      hasExceptionEntry: true,
      revisions: [
        {
          submissionId: 'SUB-001',
          stage: 'INITIAL',
          reviewerId: 'reviewer-1',
          status: 'SUPERSEDED',
          revisionNo: 1,
          totalScore: undefined,
          comment: undefined,
          submittedAt: '2026-04-01T08:00:00',
          supersededBySubmissionId: 'SUB-002',
          isCurrentEffective: false
        },
        {
          submissionId: 'SUB-002',
          stage: 'REREVIEW',
          reviewerId: 'rereviewer-1',
          status: 'SUBMITTED',
          revisionNo: 2,
          totalScore: undefined,
          comment: undefined,
          submittedAt: '2026-04-02T08:00:00',
          supersededBySubmissionId: null,
          isCurrentEffective: true
        }
      ]
    });
  });
});
