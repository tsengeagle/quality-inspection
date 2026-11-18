import { HttpErrorResponse } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { toExportRange } from '../report-export/export-date-range';
import { DashboardSummary, ReportDashboardService } from '../services/report-dashboard.service';
import { ReportDashboardPageComponent } from './report-dashboard-page.component';

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

function summary(overrides: Partial<DashboardSummary> = {}): DashboardSummary {
  return {
    totalCases: 250, closedCases: 200, rereviewedCases: 50, overdueCases: 25,
    completionRate: 0.8, rereviewRate: 0.2, overdueRate: 0.1,
    ...overrides
  };
}

const PREVIOUS_MONTH = toExportRange('2026-10-01', '2026-10-31');

describe('ReportDashboardPageComponent (UI-015)', () => {
  let fixture: ComponentFixture<ReportDashboardPageComponent>;
  let component: ReportDashboardPageComponent;
  let dashboardService: jasmine.SpyObj<ReportDashboardService>;
  let actor: ActorContextServiceStub;

  const el = (testId: string): HTMLElement | null =>
    fixture.nativeElement.querySelector(`[data-testid="${testId}"]`);
  const text = (testId: string): string => el(testId)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  const button = (testId: string): HTMLButtonElement => el(testId) as HTMLButtonElement;

  function create(): void {
    fixture = TestBed.createComponent(ReportDashboardPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function query(): void {
    button('dashboard-query').click();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 10, 17, 9, 0));
    dashboardService = jasmine.createSpyObj<ReportDashboardService>('ReportDashboardService', ['fetchSummary']);
    dashboardService.fetchSummary.and.returnValue(of(summary()));
    actor = new ActorContextServiceStub();

    await TestBed.configureTestingModule({
      declarations: [ReportDashboardPageComponent],
      imports: [FormsModule, RouterTestingModule],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        { provide: ReportDashboardService, useValue: dashboardService },
        { provide: ActorContextService, useValue: actor }
      ]
    }).compileComponents();
  });

  afterEach(() => jasmine.clock().uninstall());

  describe('page composition (GOV-BND-001)', () => {
    it('uses system-container with header and main blocks as the page root', () => {
      create();
      const root = fixture.nativeElement.firstElementChild as HTMLElement;
      expect(root.tagName.toLowerCase()).toBe('system-container');
      expect(root.getAttribute('container-type')).toBe('main');
      const blocks = Array.from(root.children).map(b => `${b.tagName.toLowerCase()}[${b.getAttribute('block-type')}]`);
      expect(blocks).toEqual(['system-block[header]', 'system-block[main]']);
      expect(root.querySelector('system-block[block-type="header"] [data-testid="dashboard-conditions"]')).not.toBeNull();
    });
  });

  describe('conditions', () => {
    it('queries once on entry with the previous full month (end date inclusive)', () => {
      create();
      expect(component.form.startDate).toBe('2026-10-01');
      expect(component.form.endDate).toBe('2026-10-31');
      expect(dashboardService.fetchSummary).toHaveBeenCalledOnceWith({ ...PREVIOUS_MONTH });
      expect(text('dashboard-scope')).toContain('2026-10-01 ～ 2026-10-31（含迄日）');
    });

    it('sends no from/to after 不限期間 and shows it in the scope summary', () => {
      create();
      button('dashboard-clear-range').click();
      fixture.detectChanges();
      expect(component.form.startDate).toBe('');
      expect(component.form.endDate).toBe('');
      query();
      expect(dashboardService.fetchSummary.calls.mostRecent().args[0]).toEqual({});
      expect(text('dashboard-scope')).toContain('不限期間');
    });

    it('disables query when only one date is given', () => {
      create();
      component.form.endDate = '';
      fixture.detectChanges();
      expect(button('dashboard-query').disabled).toBeTrue();
      expect(text('dashboard-range-error')).toContain('起迄日需同時填寫');
    });

    it('disables query when the start date is after the end date', () => {
      create();
      component.form.startDate = '2026-11-01';
      fixture.detectChanges();
      expect(button('dashboard-query').disabled).toBeTrue();
      expect(text('dashboard-range-error')).toContain('起日不可晚於迄日');
    });

    it('sends projectId when given and shows it in the scope summary', () => {
      create();
      expect(text('dashboard-scope')).toContain('全部專案');
      component.form.projectId = 'PRJ-001';
      query();
      expect(dashboardService.fetchSummary.calls.mostRecent().args[0]).toEqual({ ...PREVIOUS_MONTH, projectId: 'PRJ-001' });
      expect(text('dashboard-scope')).toContain('PRJ-001');
    });

    it('shows the hospital field only to accounts holding SYSTEM_ADMIN; others see 本院', () => {
      create();
      expect(el('dashboard-hospital')).toBeNull();
      expect(text('dashboard-scope')).toContain('本院');
    });

    it('lets SYSTEM_ADMIN (including multi-role accounts) pick a hospital or all hospitals', () => {
      actor.setContext({ roles: ['SYSTEM_ADMIN', 'HOSPITAL_MANAGER'], currentRole: 'HOSPITAL_MANAGER' });
      create();
      expect(el('dashboard-hospital')).not.toBeNull();
      expect(text('dashboard-scope')).toContain('全部院區');
      expect(dashboardService.fetchSummary.calls.mostRecent().args[0].hospitalId).toBeUndefined();

      component.form.hospitalId = 'H002';
      query();
      expect(dashboardService.fetchSummary.calls.mostRecent().args[0].hospitalId).toBe('H002');
      expect(text('dashboard-scope')).toContain('H002');
    });
  });

  describe('result', () => {
    it('shows each count and rate from the API with numerator and denominator, without recomputing', () => {
      dashboardService.fetchSummary.and.returnValue(of(summary({ completionRate: 0.123 })));
      create();
      expect(text('dashboard-card-totalCases')).toContain('250');
      expect(text('dashboard-card-closedCases')).toContain('200');
      expect(text('dashboard-card-rereviewedCases')).toContain('50');
      expect(text('dashboard-card-overdueCases')).toContain('25');
      // 0.123 不等於 200/250：證明比率取自 API，不是前端重算
      expect(text('dashboard-card-completionRate')).toContain('12.3%');
      expect(text('dashboard-card-completionRate')).toContain('200 / 250');
      expect(text('dashboard-card-rereviewRate')).toContain('20.0%');
      expect(text('dashboard-card-rereviewRate')).toContain('50 / 250');
      expect(text('dashboard-card-overdueRate')).toContain('10.0%');
      expect(text('dashboard-card-overdueRate')).toContain('25 / 250');
    });

    it('states the definitions that are easy to misread', () => {
      create();
      expect(text('dashboard-card-overdueCases')).toContain('查詢當下');
      expect(text('dashboard-card-rereviewedCases')).toContain('已指派複審醫師');
    });

    it('shows the illustration-based empty state when there are no cases', () => {
      dashboardService.fetchSummary.and.returnValue(of(summary({
        totalCases: 0, closedCases: 0, rereviewedCases: 0, overdueCases: 0,
        completionRate: 0, rereviewRate: 0, overdueRate: 0
      })));
      create();
      expect(text('dashboard-empty')).toBe('此條件沒有案件');
      expect(el('dashboard-empty')?.closest('.flex-center')?.querySelector('img')?.getAttribute('src'))
        .toBe('assets/img/Empty.svg');
      expect(el('dashboard-card-totalCases')).toBeNull();
      expect(el('dashboard-scope')).not.toBeNull();
    });

    it('links to the case list and the export page', () => {
      create();
      expect(el('dashboard-link-cases')?.getAttribute('href')).toBe('/cases');
      expect(el('dashboard-link-export')?.getAttribute('href')).toBe('/reports/export');
    });

    it('shows the backend message on 400', () => {
      dashboardService.fetchSummary.and.returnValue(throwError(() => new HttpErrorResponse({
        status: 400, error: { success: false, error: { code: 'BAD_REQUEST', message: 'from must be earlier than to' } }
      })));
      create();
      expect(text('dashboard-error')).toBe('from must be earlier than to');
    });

    it('shows a generic message on other errors and keeps the conditions', () => {
      dashboardService.fetchSummary.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      create();
      expect(text('dashboard-error')).toBe('查詢失敗，請稍後再試');
      expect(component.form.startDate).toBe('2026-10-01');
    });

    it('shows the forbidden notice on 403 and no numbers', () => {
      dashboardService.fetchSummary.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
      create();
      expect(el('dashboard-forbidden')).not.toBeNull();
      expect(el('dashboard-card-totalCases')).toBeNull();
    });

    it('disables conditions and buttons while loading', () => {
      const pending = new Subject<DashboardSummary>();
      dashboardService.fetchSummary.and.returnValue(pending.asObservable());
      create();
      expect(el('dashboard-loading')).not.toBeNull();
      expect(button('dashboard-query').disabled).toBeTrue();
      expect(button('dashboard-clear-range').disabled).toBeTrue();
      expect(component.controlsLocked).toBeTrue();
      pending.next(summary());
      pending.complete();
      fixture.detectChanges();
      expect(component.controlsLocked).toBeFalse();
    });
  });

  // UI-015 §6／D-033
  describe('actor context change', () => {
    it('drops the in-flight response of the previous identity and re-queries with the new one', () => {
      const first = new Subject<DashboardSummary>();
      dashboardService.fetchSummary.and.returnValue(first.asObservable());
      create();

      const second = new Subject<DashboardSummary>();
      dashboardService.fetchSummary.and.returnValue(second.asObservable());
      actor.setContext({ userId: 'U-2' });
      fixture.detectChanges();
      expect(dashboardService.fetchSummary).toHaveBeenCalledTimes(2);

      first.next(summary({ totalCases: 999 }));
      fixture.detectChanges();
      expect(component.summary).toBeNull();

      second.next(summary({ totalCases: 7 }));
      fixture.detectChanges();
      expect(component.summary?.totalCases).toBe(7);
    });

    it('clears the numbers immediately when the identity changes', () => {
      create();
      expect(el('dashboard-card-totalCases')).not.toBeNull();
      dashboardService.fetchSummary.and.returnValue(new Subject<DashboardSummary>().asObservable());
      actor.setContext({ hospitalId: 'H002' });
      fixture.detectChanges();
      expect(el('dashboard-card-totalCases')).toBeNull();
      expect(component.summary).toBeNull();
    });

    it('does not re-query when the context is re-emitted unchanged', () => {
      create();
      actor.setContext({});
      fixture.detectChanges();
      expect(dashboardService.fetchSummary).toHaveBeenCalledTimes(1);
      expect(component.summary).not.toBeNull();
    });

    it('clears the hospital condition when SYSTEM_ADMIN is no longer held', () => {
      actor.setRole('SYSTEM_ADMIN');
      create();
      component.form.hospitalId = 'H002';
      actor.setRole('AUDITOR');
      fixture.detectChanges();
      expect(component.form.hospitalId).toBe('');
      expect(dashboardService.fetchSummary.calls.mostRecent().args[0].hospitalId).toBeUndefined();
    });

    it('does not re-query with an invalid range after the identity changes', () => {
      create();
      component.form.endDate = '';
      actor.setContext({ userId: 'U-2' });
      fixture.detectChanges();
      expect(dashboardService.fetchSummary).toHaveBeenCalledTimes(1);
      expect(component.summary).toBeNull();
      expect(text('dashboard-idle')).toBe('請按查詢');
    });
  });
});
