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

  it('maps Wave 5 initial/rereview bootstrap truth from case detail response', () => {
    let actual: unknown;

    service.getCaseDetail('CASE-012').subscribe(result => {
      actual = result;
    });

    const request = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-012`);
    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      data: {
        caseId: 'CASE-012',
        projectId: 'PROJECT-001',
        batchId: 'BATCH-001',
        hospitalId: 'H001',
        status: 'REREVIEW_IN_PROGRESS',
        reviewerId: 'reviewer-1',
        rereviewerId: 'rereviewer-1',
        medicalRecordType: 'OUTPATIENT',
        sourceCaseKey: 'SK-012',
        submissions: [
          {
            submissionId: 'SUB-INITIAL-001',
            stage: 'INITIAL',
            reviewerId: 'reviewer-1',
            status: 'SUBMITTED',
            revisionNo: 1,
            totalScore: 88,
            submittedAt: '2026-04-01T08:00:00'
          }
        ],
        latestRereviewSubmissionId: 'SUB-REREVIEW-001',
        projectionType: 'FULL',
        actorRole: 'REREVIEWER',
        isOwner: true,
        initialReviewContext: {
          pageMode: 'REVISION',
          hasDraft: false,
          hasSubmittedBase: true,
          readonly: false,
          blocked: false,
          blockedReason: null,
          scoreSheetVersionId: 'SS-001',
          allowedActions: ['REVISE_INITIAL_REVIEW'],
          currentSubmission: {
            submissionId: 'SUB-INITIAL-001',
            revisionNo: 1,
            status: 'SUBMITTED',
            submittedAt: '2026-04-01T08:00:00'
          },
          editableSubmission: {
            submissionId: 'SUB-INITIAL-001',
            revisionNo: 1,
            status: 'SUBMITTED',
            submittedAt: '2026-04-01T08:00:00'
          },
          basedOnVersion: {
            submissionId: 'SUB-INITIAL-001',
            revisionNo: 1,
            status: 'SUBMITTED',
            submittedAt: '2026-04-01T08:00:00'
          }
        },
        rereviewContext: {
          pageMode: 'EDIT_DRAFT',
          hasDraft: true,
          hasSubmittedBase: true,
          closedRevisionException: false,
          readonly: false,
          blocked: false,
          blockedReason: null,
          scoreSheetVersionId: 'SS-001',
          allowedActions: ['SAVE_REREVIEW_DRAFT', 'SUBMIT_REREVIEW'],
          currentSubmission: {
            submissionId: 'SUB-REREVIEW-001',
            revisionNo: 1,
            status: 'DRAFT',
            submittedAt: null
          },
          editableSubmission: {
            submissionId: 'SUB-REREVIEW-001',
            revisionNo: 1,
            status: 'DRAFT',
            submittedAt: null
          },
          basedOnVersion: {
            submissionId: 'SUB-REREVIEW-BASE-001',
            revisionNo: 1,
            status: 'SUBMITTED',
            submittedAt: '2026-04-01T09:00:00'
          },
          initialSubmissionId: 'SUB-INITIAL-001',
          initialSubmissionRevisionNo: 1,
          initialReviewerId: 'reviewer-1',
          initialReviewerName: 'Dr. Initial',
          initialTotalScore: 88,
          initialSubmittedAt: '2026-04-01T08:00:00',
          finalScore: 91,
          finalConclusion: 'PASS'
        }
      }
    });

    expect(actual).toEqual({
      caseId: 'CASE-012',
      status: 'REREVIEW_IN_PROGRESS',
      projectionType: 'FULL',
      actorRole: 'REREVIEWER',
      isOwner: true,
      batchId: 'BATCH-001',
      hospitalId: 'H001',
      reviewerId: 'reviewer-1',
      rereviewerId: 'rereviewer-1',
      medicalRecordType: 'OUTPATIENT',
      sourceCaseKey: 'SK-012',
      initialReviewScore: undefined,
      finalScore: undefined,
      finalConclusion: undefined,
      createdAt: undefined,
      assignedAt: undefined,
      dueAt: undefined,
      closedAt: undefined,
      cancelledAt: undefined,
      submissions: [
        {
          submissionId: 'SUB-INITIAL-001',
          stage: 'INITIAL',
          reviewerId: 'reviewer-1',
          status: 'SUBMITTED',
          revisionNo: 1,
          totalScore: 88,
          comment: undefined,
          submittedAt: '2026-04-01T08:00:00',
          supersededBySubmissionId: undefined,
          isCurrentEffective: true
        }
      ],
      latestRereviewSubmissionId: 'SUB-REREVIEW-001',
      initialReviewContext: {
        pageMode: 'REVISION',
        hasDraft: false,
        hasSubmittedBase: true,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['REVISE_INITIAL_REVIEW'],
        currentSubmission: {
          submissionId: 'SUB-INITIAL-001',
          version: 1,
          status: 'SUBMITTED',
          submittedAt: '2026-04-01T08:00:00'
        },
        editableSubmission: {
          submissionId: 'SUB-INITIAL-001',
          version: 1,
          status: 'SUBMITTED',
          submittedAt: '2026-04-01T08:00:00'
        },
        basedOnVersion: {
          submissionId: 'SUB-INITIAL-001',
          version: 1,
          status: 'SUBMITTED',
          submittedAt: '2026-04-01T08:00:00'
        }
      },
      rereviewContext: {
        pageMode: 'EDIT_DRAFT',
        hasDraft: true,
        hasSubmittedBase: true,
        closedRevisionException: false,
        readonly: false,
        blocked: false,
        blockedReason: null,
        scoreSheetVersionId: 'SS-001',
        allowedActions: ['SAVE_REREVIEW_DRAFT', 'SUBMIT_REREVIEW'],
        currentSubmission: {
          submissionId: 'SUB-REREVIEW-001',
          version: 1,
          status: 'DRAFT',
          submittedAt: undefined
        },
        editableSubmission: {
          submissionId: 'SUB-REREVIEW-001',
          version: 1,
          status: 'DRAFT',
          submittedAt: undefined
        },
        basedOnVersion: {
          submissionId: 'SUB-REREVIEW-BASE-001',
          version: 1,
          status: 'SUBMITTED',
          submittedAt: '2026-04-01T09:00:00'
        },
        initialSubmissionId: 'SUB-INITIAL-001',
        initialSubmissionRevisionNo: 1,
        initialReviewerId: 'reviewer-1',
        initialReviewerName: 'Dr. Initial',
        initialTotalScore: 88,
        initialSubmittedAt: '2026-04-01T08:00:00',
        finalScore: 91,
        finalConclusion: 'PASS'
      }
    });
  });

  it('maps submission detail truth for minimal command payload reuse', () => {
    let actual: unknown;

    service.getSubmissionDetail('SUB-INITIAL-001').subscribe(result => {
      actual = result;
    });

    const request = httpMock.expectOne(`${environment.apiBasePath}/review-submissions/SUB-INITIAL-001`);
    expect(request.request.method).toBe('GET');
    request.flush({
      success: true,
      data: {
        submissionId: 'SUB-INITIAL-001',
        caseId: 'CASE-012',
        stage: 'INITIAL',
        revisionNo: 1,
        status: 'DRAFT',
        reviewerId: 'reviewer-1',
        answers: [
          { itemId: 'ITEM-1', answerValue: 'Y', derivedScore: 5 }
        ],
        comment: 'draft comment',
        totalScore: 5,
        maskedFields: []
      }
    });

    expect(actual).toEqual({
      submissionId: 'SUB-INITIAL-001',
      caseId: 'CASE-012',
      stage: 'INITIAL',
      revisionNo: 1,
      status: 'DRAFT',
      reviewerId: 'reviewer-1',
      answers: [
        { itemId: 'ITEM-1', answerValue: 'Y', derivedScore: 5 }
      ],
      comment: 'draft comment',
      attachmentRefsJson: undefined,
      totalScore: 5,
      createdAt: undefined,
      submittedAt: undefined,
      maskedFields: []
    });
  });

  it('maps initial-review command endpoints and payloads', () => {
    const savePayload = {
      answers: [{ itemId: 'ITEM-1', answerValue: 'Y' }],
      comment: 'save draft'
    };
    const revisePayload = {
      answers: [{ itemId: 'ITEM-2', answerValue: 'N' }],
      comment: 'revise comment'
    };
    const actual: unknown[] = [];

    service.startInitialReview('CASE-012').subscribe(result => actual.push(result));
    service.saveInitialReviewDraft('CASE-012', savePayload).subscribe(result => actual.push(result));
    service.submitInitialReview('CASE-012').subscribe(result => actual.push(result));
    service.reviseInitialReview('CASE-012', 'SUB-INITIAL-001', revisePayload).subscribe(result => actual.push(result));

    const startRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-012/initial-review/start`);
    expect(startRequest.request.method).toBe('POST');
    expect(startRequest.request.body).toEqual({});
    startRequest.flush({ success: true, data: { submissionId: 'SUB-START-001' } });

    const saveRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-012/initial-review/draft`);
    expect(saveRequest.request.method).toBe('POST');
    expect(saveRequest.request.body).toEqual(savePayload);
    saveRequest.flush({ success: true, data: { submissionId: 'SUB-DRAFT-001', status: 'DRAFT', stage: 'INITIAL', totalScore: 5 } });

    const submitRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-012/initial-review/submit`);
    expect(submitRequest.request.method).toBe('POST');
    expect(submitRequest.request.body).toEqual({});
    submitRequest.flush({
      success: true,
      data: {
        submissionId: 'SUB-DRAFT-001',
        status: 'SUBMITTED',
        caseId: 'CASE-012',
        caseStatus: 'PENDING_REREVIEW',
        totalScore: 91,
        rereviewRequired: true
      }
    });

    const reviseRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-012/initial-review/submissions/SUB-INITIAL-001/revise`);
    expect(reviseRequest.request.method).toBe('POST');
    expect(reviseRequest.request.body).toEqual(revisePayload);
    reviseRequest.flush({
      success: true,
      data: {
        newSubmissionId: 'SUB-INITIAL-002',
        oldSubmissionId: 'SUB-INITIAL-001',
        revisionNo: 2,
        totalScore: 93,
        status: 'SUBMITTED',
        stage: 'INITIAL',
        caseId: 'CASE-012'
      }
    });

    expect(actual).toEqual([
      { submissionId: 'SUB-START-001' },
      { submissionId: 'SUB-DRAFT-001', status: 'DRAFT', stage: 'INITIAL', totalScore: 5 },
      {
        submissionId: 'SUB-DRAFT-001',
        caseId: 'CASE-012',
        caseStatus: 'PENDING_REREVIEW',
        status: 'SUBMITTED',
        totalScore: 91,
        rereviewRequired: true
      },
      {
        caseId: 'CASE-012',
        status: 'SUBMITTED',
        stage: 'INITIAL',
        totalScore: 93,
        revisionNo: 2,
        oldSubmissionId: 'SUB-INITIAL-001',
        newSubmissionId: 'SUB-INITIAL-002',
        submissionId: 'SUB-INITIAL-002'
      }
    ]);
  });

  it('maps rereview command endpoints and payloads', () => {
    const draftPayload = {
      submissionId: 'SUB-REREVIEW-001',
      answers: [{ itemId: 'ITEM-1', answerValue: 'Y' }],
      comment: 'draft comment',
      totalScore: 88
    };
    const submitPayload = {
      submissionId: 'SUB-REREVIEW-001',
      answers: [{ itemId: 'ITEM-1', answerValue: 'Y' }],
      comment: 'submit comment'
    };
    const revisePayload = {
      answers: [{ itemId: 'ITEM-1', answerValue: 'N' }],
      comment: 'revise comment'
    };
    const actual: unknown[] = [];

    service.startRereview('CASE-013').subscribe(result => actual.push(result));
    service.saveRereviewDraft('CASE-013', draftPayload).subscribe(result => actual.push(result));
    service.submitRereview('CASE-013', submitPayload).subscribe(result => actual.push(result));
    service.reviseRereview('CASE-013', 'SUB-REREVIEW-001', revisePayload).subscribe(result => actual.push(result));

    const startRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-013/start-rereview`);
    expect(startRequest.request.method).toBe('POST');
    expect(startRequest.request.body).toEqual({});
    startRequest.flush({ success: true, data: null });

    const saveRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-013/rereview/draft`);
    expect(saveRequest.request.method).toBe('POST');
    expect(saveRequest.request.body).toEqual(draftPayload);
    saveRequest.flush({ success: true, data: { submissionId: 'SUB-REREVIEW-001', status: 'DRAFT', stage: 'REREVIEW', totalScore: 88 } });

    const submitRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-013/rereview/submit`);
    expect(submitRequest.request.method).toBe('POST');
    expect(submitRequest.request.body).toEqual(submitPayload);
    submitRequest.flush({
      success: true,
      data: {
        submissionId: 'SUB-REREVIEW-001',
        status: 'SUBMITTED',
        caseId: 'CASE-013',
        caseStatus: 'CLOSED',
        finalScore: 84
      }
    });

    const reviseRequest = httpMock.expectOne(`${environment.apiBasePath}/review-cases/CASE-013/rereview/submissions/SUB-REREVIEW-001/revise`);
    expect(reviseRequest.request.method).toBe('POST');
    expect(reviseRequest.request.body).toEqual(revisePayload);
    reviseRequest.flush({
      success: true,
      data: {
        newSubmissionId: 'SUB-REREVIEW-002',
        oldSubmissionId: 'SUB-REREVIEW-001',
        revisionNo: 2,
        totalScore: 84,
        status: 'SUBMITTED',
        stage: 'REREVIEW',
        caseId: 'CASE-013'
      }
    });

    expect(actual).toEqual([
      { caseId: 'CASE-013' },
      { submissionId: 'SUB-REREVIEW-001', status: 'DRAFT', stage: 'REREVIEW', totalScore: 88 },
      {
        submissionId: 'SUB-REREVIEW-001',
        caseId: 'CASE-013',
        caseStatus: 'CLOSED',
        status: 'SUBMITTED',
        finalScore: 84
      },
      {
        caseId: 'CASE-013',
        status: 'SUBMITTED',
        stage: 'REREVIEW',
        totalScore: 84,
        revisionNo: 2,
        oldSubmissionId: 'SUB-REREVIEW-001',
        newSubmissionId: 'SUB-REREVIEW-002',
        submissionId: 'SUB-REREVIEW-002'
      }
    ]);
  });
});
