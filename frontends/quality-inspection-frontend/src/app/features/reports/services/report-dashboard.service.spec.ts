import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '@env/environment';
import { DashboardSummary, ReportDashboardService } from './report-dashboard.service';

const URL = `${environment.apiBasePath}/reports/dashboard-summary`;

const SUMMARY: DashboardSummary = {
  totalCases: 250, closedCases: 200, rereviewedCases: 50, overdueCases: 25,
  completionRate: 0.8, rereviewRate: 0.2, overdueRate: 0.1
};

// UI-015 §3.1、§4；api-spec 15.1
describe('ReportDashboardService', () => {
  let service: ReportDashboardService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ReportDashboardService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends from, to, projectId and hospitalId as query params and returns the summary', () => {
    let result: DashboardSummary | undefined;
    service.fetchSummary({
      from: '2026-09-30T16:00:00.000Z', to: '2026-10-31T16:00:00.000Z', projectId: 'PRJ-001', hospitalId: 'H002'
    }).subscribe(r => (result = r));

    const req = http.expectOne(r => r.url === URL);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('from')).toBe('2026-09-30T16:00:00.000Z');
    expect(req.request.params.get('to')).toBe('2026-10-31T16:00:00.000Z');
    expect(req.request.params.get('projectId')).toBe('PRJ-001');
    expect(req.request.params.get('hospitalId')).toBe('H002');
    req.flush({ success: true, data: SUMMARY });

    expect(result).toEqual(SUMMARY);
  });

  it('omits every param that has no value (不限期間, all projects, all hospitals)', () => {
    service.fetchSummary({ projectId: '  ', hospitalId: '' }).subscribe();
    const req = http.expectOne(r => r.url === URL);
    expect(req.request.params.keys()).toEqual([]);
    req.flush({ success: true, data: SUMMARY });
  });

  it('trims projectId and hospitalId', () => {
    service.fetchSummary({ projectId: ' PRJ-001 ', hospitalId: ' H002 ' }).subscribe();
    const req = http.expectOne(r => r.url === URL);
    expect(req.request.params.get('projectId')).toBe('PRJ-001');
    expect(req.request.params.get('hospitalId')).toBe('H002');
    req.flush({ success: true, data: SUMMARY });
  });
});
