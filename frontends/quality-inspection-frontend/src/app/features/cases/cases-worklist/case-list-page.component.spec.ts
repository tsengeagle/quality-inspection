import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CasesService } from '../services/cases.service';
import { CASE_STATUSES } from '../models/case-status';
import { CaseListPageComponent } from './case-list-page.component';

class ActivatedRouteStub {
  private readonly queryParamsSubject = new BehaviorSubject<Params>({});

  readonly queryParams = this.queryParamsSubject.asObservable();

  setQueryParams(params: Params): void {
    this.queryParamsSubject.next(params);
  }
}

class ActorContextServiceStub {
  private readonly subject = new BehaviorSubject({
    userId: 'U-REVIEWER',
    userName: 'Reviewer',
    roles: ['REVIEWER'] as string[],
    currentRole: 'REVIEWER' as string | null,
    hospitalId: 'H-001'
  });

  readonly context$ = this.subject.asObservable();

  setRole(role: string): void {
    this.subject.next({
      userId: 'U-REVIEWER',
      userName: 'Reviewer',
      roles: [role],
      currentRole: role,
      hospitalId: 'H-001'
    });
  }

  resolvePrimaryRole(fallbackRole: string): string {
    return this.subject.getValue().currentRole ?? fallbackRole;
  }

  isManagementRole(role: string | null | undefined): boolean {
    return role === 'HOSPITAL_MANAGER';
  }

  isMetadataOnlyRole(role: string | null | undefined): boolean {
    return role === 'SYSTEM_ADMIN';
  }
}

describe('CaseListPageComponent', () => {
  let fixture: ComponentFixture<CaseListPageComponent>;
  let component: CaseListPageComponent;
  let activatedRoute: ActivatedRouteStub;
  let router: jasmine.SpyObj<Router>;
  let casesService: jasmine.SpyObj<CasesService>;
  let actorContextService: ActorContextServiceStub;

  beforeEach(async () => {
    activatedRoute = new ActivatedRouteStub();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    casesService = jasmine.createSpyObj<CasesService>('CasesService', ['getCases']);
    actorContextService = new ActorContextServiceStub();
    casesService.getCases.and.returnValue(of({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      projectionType: 'FULL'
    }));

    await TestBed.configureTestingModule({
      declarations: [CaseListPageComponent],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: Router, useValue: router },
        { provide: CasesService, useValue: casesService },
        { provide: ActorContextService, useValue: actorContextService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseListPageComponent);
    component = fixture.componentInstance;
  });

  it('resolves metadata-only mode from backend list truth and hides business commands', () => {
    casesService.getCases.and.returnValue(of({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      projectionType: 'METADATA_ONLY'
    }));
    activatedRoute.setQueryParams({});

    fixture.detectChanges();

    expect(component.uiState.tab).toBe('MY_QUEUE');
    expect(component.showBusinessCommands).toBeFalse();
    expect(component.showProjectionNotice).toBeTrue();
    expect(component.modeLabel).toBe('唯讀（metadata-only）');
    expect(casesService.getCases).toHaveBeenCalledWith(jasmine.objectContaining({
      role: 'REVIEWER',
      status: 'PENDING_INITIAL_REVIEW',
      page: 0,
      size: 20
    }));
  });

  it('uses the manager default tab and persists tab changes through query params', () => {
    actorContextService.setRole('HOSPITAL_MANAGER');
    activatedRoute.setQueryParams({});

    fixture.detectChanges();
    component.onTabChange('MY_MANAGED_PROJECTS');

    expect(component.uiState.tab).toBe('MY_MANAGED_PROJECTS');
    expect(component.modeLabel).toBe('管理者視角');
    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: activatedRoute as unknown as ActivatedRoute,
      queryParams: { tab: 'MY_MANAGED_PROJECTS', status: 'SELECTED_PENDING_ASSIGNMENT' },
      queryParamsHandling: 'merge'
    });
  });

  // 抽選執行頁、批次清單導覽過來時網址帶 batchId；原本列表頁沒有把它交給查詢
  it('passes the batchId query param to the list query and shows it as an applied filter', () => {
    actorContextService.setRole('HOSPITAL_MANAGER');
    activatedRoute.setQueryParams({ batchId: 'BATCH-007' });

    fixture.detectChanges();

    expect(component.uiState.batchId).toBe('BATCH-007');
    expect(casesService.getCases).toHaveBeenCalledWith(jasmine.objectContaining({ batchId: 'BATCH-007' }));
    const chip = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="case-list-batch-filter"]');
    expect(chip?.textContent).toContain('BATCH-007');
  });

  it('does not send a batchId and shows no batch filter when the query param is absent', () => {
    activatedRoute.setQueryParams({});

    fixture.detectChanges();

    expect(component.uiState.batchId).toBeUndefined();
    expect(casesService.getCases.calls.mostRecent().args[0].batchId).toBeUndefined();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="case-list-batch-filter"]')).toBeNull();
  });

  it('clears the batch filter by removing the query param', () => {
    activatedRoute.setQueryParams({ batchId: 'BATCH-007' });
    fixture.detectChanges();

    component.clearBatchFilter();

    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: activatedRoute as unknown as ActivatedRoute,
      queryParams: { batchId: null },
      queryParamsHandling: 'merge'
    });
  });

  it('never sends a status the backend rejects, whatever the tab', () => {
    for (const role of ['REVIEWER', 'REREVIEWER', 'HOSPITAL_MANAGER', 'AUDITOR', 'SYSTEM_ADMIN']) {
      for (const tab of ['MY_QUEUE', 'ALL_CASES', 'MY_MANAGED_PROJECTS']) {
        actorContextService.setRole(role);
        casesService.getCases.calls.reset();
        fixture = TestBed.createComponent(CaseListPageComponent);
        activatedRoute.setQueryParams({ tab });
        fixture.detectChanges();

        const status = casesService.getCases.calls.mostRecent().args[0].status;
        if (status !== undefined) {
          expect(CASE_STATUSES as readonly string[]).withContext(`${role} ${tab}`).toContain(status);
        }
      }
    }
  });
});

