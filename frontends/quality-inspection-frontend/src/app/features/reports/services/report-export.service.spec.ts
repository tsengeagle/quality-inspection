import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '@env/environment';
import {
  EXPORT_DOWNLOAD_PAGE_SIZE,
  ExportDownloadError,
  ExportPageResult,
  ExportRowView,
  ReportExportService
} from './report-export.service';

const URL = `${environment.apiBasePath}/reports/export`;

const SUMMARY = {
  totalCases: 3, closedCases: 2, rereviewedCases: 1, overdueCases: 0,
  completionRate: 2 / 3, rereviewRate: 1 / 3, overdueRate: 0
};

function rowDto(caseId: string) {
  return {
    caseId, projectId: 'PRJ-001', hospitalId: 'H001', status: 'CLOSED',
    reviewerId: 'reviewer-1', rereviewerId: null, initialScore: null, finalScore: 88,
    createdAt: '2026-10-02T01:00:00Z', closedAt: '2026-10-05T01:30:00Z',
    maskedFields: ['initialScore']
  };
}

function pageBody(page: number, totalElements: number, totalPages: number, caseIds: string[]) {
  return {
    success: true,
    data: {
      reportType: 'REVIEW_COMPLETION', rows: caseIds.map(rowDto), summary: SUMMARY,
      page, size: EXPORT_DOWNLOAD_PAGE_SIZE, totalElements, totalPages
    }
  };
}

const CRITERIA = { from: '2026-09-30T16:00:00.000Z', to: '2026-10-31T16:00:00.000Z' };

describe('ReportExportService', () => {
  let service: ReportExportService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ReportExportService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('fetchPage', () => {
    it('posts the REVIEW_COMPLETION request with from, to, page and size and maps the response', () => {
      let result: ExportPageResult | undefined;
      service.fetchPage({ ...CRITERIA, page: 2, size: 100 }).subscribe(r => (result = r));

      const req = http.expectOne(URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        reportType: 'REVIEW_COMPLETION', from: CRITERIA.from, to: CRITERIA.to, page: 2, size: 100
      });
      req.flush(pageBody(2, 3, 1, ['CASE-001']));

      expect(result?.rows[0]).toEqual(rowDto('CASE-001') as ExportRowView);
      expect(result?.summary).toEqual(SUMMARY);
      expect(result?.totalElements).toBe(3);
      expect(result?.page).toBe(2);
    });

    it('sends hospitalId only when one is given', () => {
      service.fetchPage({ ...CRITERIA, hospitalId: 'H002', page: 0, size: 100 }).subscribe();
      expect(http.expectOne(URL).request.body.hospitalId).toBe('H002');

      service.fetchPage({ ...CRITERIA, hospitalId: '  ', page: 0, size: 100 }).subscribe();
      expect('hospitalId' in http.expectOne(URL).request.body).toBeFalse();
    });

    it('treats missing maskedFields as no masking', () => {
      let result: ExportPageResult | undefined;
      service.fetchPage({ ...CRITERIA, page: 0, size: 100 }).subscribe(r => (result = r));
      const body = pageBody(0, 1, 1, ['CASE-001']);
      delete (body.data.rows[0] as Partial<ReturnType<typeof rowDto>>).maskedFields;
      http.expectOne(URL).flush(body);
      expect(result?.rows[0].maskedFields).toEqual([]);
    });
  });

  // UI-017 §5.1
  describe('fetchAllRows', () => {
    it('fetches every page in order with size 1000 and reports progress', () => {
      const progress: Array<[number, number]> = [];
      let rows: ExportRowView[] | undefined;
      service.fetchAllRows(CRITERIA, (done, total) => progress.push([done, total]))
        .subscribe(r => (rows = r));

      const first = http.expectOne(URL);
      expect(first.request.body).toEqual(jasmine.objectContaining({ page: 0, size: 1000 }));
      first.flush(pageBody(0, 3, 3, ['CASE-001']));

      const second = http.expectOne(URL);
      expect(second.request.body.page).toBe(1);
      second.flush(pageBody(1, 3, 3, ['CASE-002']));

      const third = http.expectOne(URL);
      expect(third.request.body.page).toBe(2);
      third.flush(pageBody(2, 3, 3, ['CASE-003']));

      expect(rows?.map(r => r.caseId)).toEqual(['CASE-001', 'CASE-002', 'CASE-003']);
      expect(progress).toEqual([[1, 3], [2, 3], [3, 3]]);
    });

    it('stops after the first page when the range is empty', () => {
      let rows: ExportRowView[] | undefined;
      service.fetchAllRows(CRITERIA).subscribe(r => (rows = r));
      http.expectOne(URL).flush(pageBody(0, 0, 0, []));
      expect(rows).toEqual([]);
    });

    it('aborts with DATA_CHANGED when totalElements differs from the first page', () => {
      let error: unknown;
      let rows: ExportRowView[] | undefined;
      service.fetchAllRows(CRITERIA).subscribe({ next: r => (rows = r), error: e => (error = e) });

      http.expectOne(URL).flush(pageBody(0, 2, 2, ['CASE-001']));
      http.expectOne(URL).flush(pageBody(1, 3, 3, ['CASE-002']));

      expect(rows).toBeUndefined();
      expect(error).toEqual(jasmine.any(ExportDownloadError));
      expect((error as ExportDownloadError).reason).toBe('DATA_CHANGED');
      http.expectNone(URL);
    });

    it('aborts without further requests when any page fails', () => {
      let error: unknown;
      let rows: ExportRowView[] | undefined;
      service.fetchAllRows(CRITERIA).subscribe({ next: r => (rows = r), error: e => (error = e) });

      http.expectOne(URL).flush(pageBody(0, 3, 3, ['CASE-001']));
      http.expectOne(URL).flush({}, { status: 500, statusText: 'Server Error' });

      expect(rows).toBeUndefined();
      expect(error).toBeDefined();
      http.expectNone(URL);
    });
  });
});
