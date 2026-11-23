import { HttpErrorResponse } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CsvFileSaver } from '../services/csv-file-saver.service';
import {
  ExportDownloadError,
  ExportPageResult,
  ExportRowView,
  ReportExportService
} from '../services/report-export.service';
import { toExportRange } from './export-date-range';
import { ReportExportPageComponent } from './report-export-page.component';

class ActorContextServiceStub {
  readonly subject = new BehaviorSubject({
    userId: 'U-1', userName: 'User', roles: ['HOSPITAL_MANAGER'] as string[],
    currentRole: 'HOSPITAL_MANAGER' as string | null, hospitalId: 'H001'
  });
  readonly context$ = this.subject.asObservable();

  setRole(role: string): void {
    this.subject.next({ ...this.subject.getValue(), roles: [role], currentRole: role });
  }

  setContext(patch: Partial<ReturnType<ActorContextServiceStub['subject']['getValue']>>): void {
    this.subject.next({ ...this.subject.getValue(), ...patch });
  }

  getSnapshot() {
    return this.subject.getValue();
  }
}

function row(overrides: Partial<ExportRowView> = {}): ExportRowView {
  return {
    caseId: 'CASE-001', projectId: 'PRJ-001', hospitalId: 'H001', status: 'CLOSED',
    reviewerId: 'reviewer-1', rereviewerId: null, initialScore: 85.5, finalScore: 0,
    createdAt: '2026-10-02T01:00:00Z', closedAt: '2026-10-05T01:30:00Z', maskedFields: [],
    ...overrides
  };
}

function pageResult(overrides: Partial<ExportPageResult> = {}): ExportPageResult {
  return {
    reportType: 'REVIEW_COMPLETION',
    rows: [row()],
    summary: {
      totalCases: 250, closedCases: 200, rereviewedCases: 50, overdueCases: 25,
      completionRate: 0.8, rereviewRate: 0.2, overdueRate: 0.1
    },
    page: 0, size: 100, totalElements: 250, totalPages: 3,
    ...overrides
  };
}

describe('ReportExportPageComponent (UI-017)', () => {
  let fixture: ComponentFixture<ReportExportPageComponent>;
  let component: ReportExportPageComponent;
  let exportService: jasmine.SpyObj<ReportExportService>;
  let saver: jasmine.SpyObj<CsvFileSaver>;
  let actor: ActorContextServiceStub;

  const el = (testId: string): HTMLElement | null =>
    fixture.nativeElement.querySelector(`[data-testid="${testId}"]`);
  const text = (testId: string): string => el(testId)?.textContent?.trim() ?? '';
  const button = (testId: string): HTMLButtonElement => el(testId) as HTMLButtonElement;

  function create(): void {
    fixture = TestBed.createComponent(ReportExportPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function query(): void {
    button('export-query').click();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 10, 17, 9, 0));
    exportService = jasmine.createSpyObj<ReportExportService>('ReportExportService', ['fetchPage', 'fetchAllRows']);
    exportService.fetchPage.and.returnValue(of(pageResult()));
    saver = jasmine.createSpyObj<CsvFileSaver>('CsvFileSaver', ['save']);
    actor = new ActorContextServiceStub();

    await TestBed.configureTestingModule({
      declarations: [ReportExportPageComponent],
      imports: [FormsModule],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        { provide: ReportExportService, useValue: exportService },
        { provide: CsvFileSaver, useValue: saver },
        { provide: ActorContextService, useValue: actor }
      ]
    }).compileComponents();
  });

  afterEach(() => jasmine.clock().uninstall());

  describe('page composition (GOV-BND-001, design language §7)', () => {
    it('uses system-container with header and main blocks as the page root', () => {
      create();
      const root = fixture.nativeElement.firstElementChild as HTMLElement;
      expect(root.tagName.toLowerCase()).toBe('system-container');
      expect(root.getAttribute('container-type')).toBe('main');
      const blocks = Array.from(root.children).map(b => `${b.tagName.toLowerCase()}[${b.getAttribute('block-type')}]`);
      expect(blocks).toEqual(['system-block[header]', 'system-block[main]']);
      expect(root.querySelector('system-block[block-type="header"] [data-testid="export-conditions"]')).not.toBeNull();
    });

    it('uses table-custom for the result table', () => {
      create();
      query();
      expect(el('export-table')?.classList).toContain('table-custom');
    });

    it('renders the illustration-based empty state for the initial prompt', () => {
      create();
      const container = el('export-initial')?.closest('.flex-center');
      expect(container?.querySelector('img')?.getAttribute('src')).toBe('assets/img/Empty-2.svg');
    });
  });

  describe('conditions', () => {
    it('defaults to the previous full month and does not query until asked', () => {
      create();
      expect(component.form.startDate).toBe('2026-10-01');
      expect(component.form.endDate).toBe('2026-10-31');
      expect(component.form.reportType).toBe('REVIEW_COMPLETION');
      expect(exportService.fetchPage).not.toHaveBeenCalled();
      expect(text('export-initial')).toBe('請按查詢');
    });

    it('sends the end date as the next local midnight (end date inclusive)', () => {
      create();
      query();
      expect(exportService.fetchPage).toHaveBeenCalledOnceWith({
        ...toExportRange('2026-10-01', '2026-10-31'), page: 0, size: 100
      });
    });

    it('disables query and download when the start date is after the end date', () => {
      create();
      component.form.startDate = '2026-11-01';
      fixture.detectChanges();
      expect(button('export-query').disabled).toBeTrue();
      expect(button('export-download').disabled).toBeTrue();
      expect(text('export-range-error')).toContain('起日不可晚於迄日');
    });

    it('shows the hospital field only to SYSTEM_ADMIN; other roles see 本院 in the scope summary', () => {
      create();
      expect(el('export-hospital')).toBeNull();
      query();
      expect(text('export-scope')).toContain('本院');
      expect(text('export-scope')).toContain('2026-10-01 ～ 2026-10-31（含迄日）');
    });

    it('treats a multi-role account holding SYSTEM_ADMIN as SYSTEM_ADMIN, like the backend scope', () => {
      actor.setContext({ roles: ['SYSTEM_ADMIN', 'HOSPITAL_MANAGER'], currentRole: 'HOSPITAL_MANAGER' });
      create();
      expect(el('export-hospital')).not.toBeNull();
      query();
      expect(text('export-scope')).toContain('全部院區');
    });

    it('lets SYSTEM_ADMIN pick a hospital or leave it blank for all hospitals', () => {
      actor.setRole('SYSTEM_ADMIN');
      create();
      expect(el('export-hospital')).not.toBeNull();

      query();
      expect(exportService.fetchPage.calls.mostRecent().args[0].hospitalId).toBeUndefined();
      expect(text('export-scope')).toContain('全部院區');

      component.form.hospitalId = 'H002';
      query();
      expect(exportService.fetchPage.calls.mostRecent().args[0].hospitalId).toBe('H002');
      expect(text('export-scope')).toContain('H002');
    });
  });

  describe('result', () => {
    it('shows the summary and the table from the API response', () => {
      create();
      query();
      expect(text('export-summary')).toContain('250');
      expect(text('export-summary')).toContain('80.0%');
      expect(text('export-summary')).toContain('10.0%');
      expect(text('export-pagination')).toContain('第 1 / 3 頁');
      expect(text('export-pagination')).toContain('共 250 筆');
      const cells = Array.from(fixture.nativeElement.querySelectorAll('[data-testid="export-row"] td'))
        .map(c => (c as HTMLElement).textContent?.trim());
      expect(cells.slice(0, 6)).toEqual(['CASE-001', 'PRJ-001', 'H001', 'CLOSED', 'reviewer-1', '—']);
      expect(cells[6]).toBe('85.5');
      expect(cells[7]).toBe('0');
    });

    it('shows createdAt and closedAt in Taipei time (D-075)', () => {
      create();
      query();
      const cells = fixture.nativeElement.querySelectorAll('[data-testid="export-row"] td');
      expect((cells[8] as HTMLElement).textContent?.trim()).toBe('2026-10-02 09:00:00');
      expect((cells[9] as HTMLElement).textContent?.trim()).toBe('2026-10-05 09:30:00');
      expect(text('export-table')).toContain('結案時間');
      expect(text('export-table')).not.toContain('伺服器時間');
    });

    it('shows 已遮罩 for masked scores, never 0, blank or —', () => {
      exportService.fetchPage.and.returnValue(of(pageResult({
        rows: [row({ initialScore: null, finalScore: null, maskedFields: ['initialScore', 'finalScore'] })]
      })));
      create();
      query();
      const cells = fixture.nativeElement.querySelectorAll('[data-testid="export-row"] td');
      expect((cells[6] as HTMLElement).textContent?.trim()).toBe('已遮罩');
      expect((cells[7] as HTMLElement).textContent?.trim()).toBe('已遮罩');
    });

    it('shows — for a score that is null but not masked', () => {
      exportService.fetchPage.and.returnValue(of(pageResult({ rows: [row({ finalScore: null })] })));
      create();
      query();
      const cells = fixture.nativeElement.querySelectorAll('[data-testid="export-row"] td');
      expect((cells[7] as HTMLElement).textContent?.trim()).toBe('—');
    });

    it('requests the next page with the same conditions', () => {
      create();
      query();
      component.form.startDate = '2026-09-01';
      exportService.fetchPage.and.returnValue(of(pageResult({ page: 1 })));
      button('export-next-page').click();
      fixture.detectChanges();
      expect(exportService.fetchPage.calls.mostRecent().args[0]).toEqual({
        ...toExportRange('2026-10-01', '2026-10-31'), page: 1, size: 100
      });
      expect(text('export-pagination')).toContain('第 2 / 3 頁');
    });

    it('shows 此區間沒有案件 and disables download when the range is empty', () => {
      exportService.fetchPage.and.returnValue(of(pageResult({
        rows: [], totalElements: 0, totalPages: 0,
        summary: {
          totalCases: 0, closedCases: 0, rereviewedCases: 0, overdueCases: 0,
          completionRate: 0, rereviewRate: 0, overdueRate: 0
        }
      })));
      create();
      query();
      expect(text('export-empty')).toBe('此區間沒有案件');
      expect(el('export-empty')?.closest('.flex-center')?.querySelector('img')?.getAttribute('src'))
        .toBe('assets/img/Empty.svg');
      expect(el('export-summary')).not.toBeNull();
      expect(button('export-download').disabled).toBeTrue();
    });

    it('re-enables download once the conditions differ from the empty query', () => {
      exportService.fetchPage.and.returnValue(of(pageResult({ rows: [], totalElements: 0, totalPages: 0 })));
      actor.setRole('SYSTEM_ADMIN');
      create();
      query();
      expect(button('export-download').disabled).toBeTrue();

      component.form.startDate = '2026-09-01';
      fixture.detectChanges();
      expect(button('export-download').disabled).toBeFalse();

      component.form.startDate = '2026-10-01';
      fixture.detectChanges();
      expect(button('export-download').disabled).toBeTrue();

      component.form.hospitalId = 'H002';
      fixture.detectChanges();
      expect(button('export-download').disabled).toBeFalse();
    });

    it('shows the backend message on 400', () => {
      exportService.fetchPage.and.returnValue(throwError(() => new HttpErrorResponse({
        status: 400, error: { success: false, error: { code: 'BAD_REQUEST', message: 'from must be earlier than to' } }
      })));
      create();
      query();
      expect(text('export-error')).toBe('from must be earlier than to');
    });

    it('shows a generic message on other errors and keeps the conditions', () => {
      exportService.fetchPage.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      create();
      query();
      expect(text('export-error')).toBe('查詢失敗，請稍後再試');
      expect(component.form.startDate).toBe('2026-10-01');
    });

    it('shows the forbidden notice on 403', () => {
      exportService.fetchPage.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
      create();
      query();
      expect(el('export-forbidden')).not.toBeNull();
      expect(el('export-table')).toBeNull();
    });

    it('disables conditions and buttons while loading', () => {
      const pending = new Subject<ExportPageResult>();
      exportService.fetchPage.and.returnValue(pending.asObservable());
      create();
      query();
      expect(el('export-loading')).not.toBeNull();
      expect(button('export-query').disabled).toBeTrue();
      expect(button('export-download').disabled).toBeTrue();
      expect(component.controlsLocked).toBeTrue();
      pending.next(pageResult());
      pending.complete();
      fixture.detectChanges();
      expect(component.controlsLocked).toBeFalse();
    });
  });

  // D-033：身分或授權範圍改變後，不得保留上一個身分取得的資料。
  describe('actor context change', () => {
    it('clears the result and applied conditions when the user, hospital or roles change', () => {
      create();
      query();
      expect(el('export-table')).not.toBeNull();

      actor.setContext({ userId: 'U-2' });
      fixture.detectChanges();
      expect(el('export-table')).toBeNull();
      expect(component.result).toBeNull();
      expect(component.applied).toBeNull();
      expect(text('export-initial')).toBe('請按查詢');

      query();
      actor.setContext({ hospitalId: 'H002' });
      fixture.detectChanges();
      expect(component.result).toBeNull();

      query();
      actor.setRole('AUDITOR');
      fixture.detectChanges();
      expect(component.result).toBeNull();
    });

    it('keeps the result when the context is re-emitted unchanged', () => {
      create();
      query();
      actor.setContext({});
      fixture.detectChanges();
      expect(component.result).not.toBeNull();
    });

    it('drops an in-flight query response from the previous identity', () => {
      const pending = new Subject<ExportPageResult>();
      exportService.fetchPage.and.returnValue(pending.asObservable());
      create();
      query();

      actor.setContext({ userId: 'U-2' });
      pending.next(pageResult());
      fixture.detectChanges();

      expect(component.result).toBeNull();
      expect(component.status).toBe('initial');
      expect(component.controlsLocked).toBeFalse();
    });

    it('cancels an in-flight download so no file of the previous identity is saved', () => {
      const pending = new Subject<ExportRowView[]>();
      exportService.fetchAllRows.and.returnValue(pending.asObservable());
      create();
      button('export-download').click();
      fixture.detectChanges();
      expect(component.downloading).toBeTrue();

      actor.setContext({ userId: 'U-2' });
      pending.next([row()]);
      fixture.detectChanges();

      expect(saver.save).not.toHaveBeenCalled();
      expect(component.downloading).toBeFalse();
      expect(el('export-download-progress')).toBeNull();
    });

    it('clears the hospital condition when SYSTEM_ADMIN is no longer held', () => {
      actor.setRole('SYSTEM_ADMIN');
      create();
      component.form.hospitalId = 'H002';
      actor.setRole('HOSPITAL_MANAGER');
      fixture.detectChanges();
      expect(component.form.hospitalId).toBe('');
      expect(el('export-hospital')).toBeNull();
    });
  });

  describe('download', () => {
    it('fetches every row with the current conditions and saves a CSV named after the dates', () => {
      exportService.fetchAllRows.and.returnValue(of([row({ caseId: 'CASE-A' }), row({ caseId: 'CASE-B' })]));
      create();
      query();
      button('export-download').click();
      fixture.detectChanges();

      expect(exportService.fetchAllRows).toHaveBeenCalledWith(
        toExportRange('2026-10-01', '2026-10-31'), jasmine.any(Function)
      );
      expect(saver.save).toHaveBeenCalledTimes(1);
      const [filename, content] = saver.save.calls.mostRecent().args;
      expect(filename).toBe('review-completion_2026-10-01_2026-10-31.csv');
      expect(content.charAt(0)).toBe('﻿');
      expect(content).toContain('CASE-A');
      expect(content).toContain('CASE-B');
    });

    it('does not save a file and asks to retry when the data changed during download', () => {
      exportService.fetchAllRows.and.returnValue(throwError(() => new ExportDownloadError('DATA_CHANGED')));
      create();
      query();
      button('export-download').click();
      fixture.detectChanges();
      expect(saver.save).not.toHaveBeenCalled();
      expect(text('export-download-error')).toBe('資料在下載期間有變動，請重新下載');
    });

    it('does not save a file when a page fails', () => {
      exportService.fetchAllRows.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      create();
      query();
      button('export-download').click();
      fixture.detectChanges();
      expect(saver.save).not.toHaveBeenCalled();
      expect(text('export-download-error')).toBe('下載失敗，未產生檔案，請稍後再試');
    });

    it('switches to the forbidden state and drops the preview when a download page returns 403', () => {
      exportService.fetchAllRows.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
      create();
      query();
      expect(el('export-table')).not.toBeNull();
      button('export-download').click();
      fixture.detectChanges();
      expect(saver.save).not.toHaveBeenCalled();
      expect(el('export-forbidden')).not.toBeNull();
      expect(el('export-table')).toBeNull();
      expect(component.result).toBeNull();
    });

    it('shows progress and locks the controls while downloading', () => {
      const pending = new Subject<ExportRowView[]>();
      exportService.fetchAllRows.and.callFake((_criteria, onProgress) => {
        onProgress?.(1, 3);
        return pending.asObservable();
      });
      create();
      query();
      button('export-download').click();
      fixture.detectChanges();

      expect(text('export-download-progress')).toContain('1 / 3');
      expect(button('export-query').disabled).toBeTrue();
      expect(button('export-download').disabled).toBeTrue();
      expect(component.controlsLocked).toBeTrue();

      pending.next([row()]);
      pending.complete();
      fixture.detectChanges();
      expect(component.controlsLocked).toBeFalse();
      expect(el('export-download-progress')).toBeNull();
    });
  });
});
