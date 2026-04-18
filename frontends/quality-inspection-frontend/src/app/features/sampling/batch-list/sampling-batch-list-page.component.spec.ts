import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { SamplingService } from '../services/sampling.service';
import { SamplingBatchListPageComponent } from './sampling-batch-list-page.component';

class ActivatedRouteStub {
  private readonly queryParamsSubject = new BehaviorSubject<Params>({});

  readonly queryParams = this.queryParamsSubject.asObservable();

  setQueryParams(params: Params): void {
    this.queryParamsSubject.next(params);
  }
}

class ActorContextServiceStub {
  private readonly subject = new BehaviorSubject({
    userId: 'U-MANAGER',
    userName: 'Manager',
    roles: ['HOSPITAL_MANAGER'] as string[],
    currentRole: 'HOSPITAL_MANAGER' as string | null,
    hospitalId: 'H-001'
  });

  readonly context$ = this.subject.asObservable();

  setRole(role: string): void {
    this.subject.next({
      userId: 'U-MANAGER',
      userName: 'Manager',
      roles: [role],
      currentRole: role,
      hospitalId: 'H-001'
    });
  }

  resolvePrimaryRole(fallbackRole: string): string {
    return this.subject.getValue().currentRole ?? fallbackRole;
  }

  isMetadataOnlyRole(role: string | null | undefined): boolean {
    return role === 'SYSTEM_ADMIN';
  }
}

describe('SamplingBatchListPageComponent', () => {
  let fixture: ComponentFixture<SamplingBatchListPageComponent>;
  let component: SamplingBatchListPageComponent;
  let activatedRoute: ActivatedRouteStub;
  let router: jasmine.SpyObj<Router>;
  let samplingService: jasmine.SpyObj<SamplingService>;
  let actorContextService: ActorContextServiceStub;

  beforeEach(async () => {
    activatedRoute = new ActivatedRouteStub();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    samplingService = jasmine.createSpyObj<SamplingService>('SamplingService', ['getBatches']);
    actorContextService = new ActorContextServiceStub();
    samplingService.getBatches.and.returnValue(of({
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0
    }));

    await TestBed.configureTestingModule({
      declarations: [SamplingBatchListPageComponent],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: Router, useValue: router },
        { provide: SamplingService, useValue: samplingService },
        { provide: ActorContextService, useValue: actorContextService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(SamplingBatchListPageComponent);
    component = fixture.componentInstance;
  });

  it('resolves the role-tab matrix baseline for HOSPITAL_MANAGER', () => {
    component.role = 'HOSPITAL_MANAGER';
    activatedRoute.setQueryParams({});

    fixture.detectChanges();

    expect(component.uiState.tab).toBe('ALL_BATCHES');
    expect(component.availableTabs).toEqual([
      { value: 'ALL_BATCHES', label: '全部批次' },
      { value: 'COMPLETED', label: '成功' },
      { value: 'FAILED', label: '失敗' },
      { value: 'RETRYABLE', label: '可重跑' }
    ]);
  });

  it('resolves the SYSTEM_ADMIN metadata-only baseline', () => {
    actorContextService.setRole('SYSTEM_ADMIN');
    activatedRoute.setQueryParams({});

    fixture.detectChanges();

    expect(component.isMetadataOnly).toBeTrue();
    expect(component.uiState.tab).toBe('ALL_BATCHES');
    expect(component.availableTabs).toEqual([
      { value: 'ALL_BATCHES', label: '全部批次' },
      { value: 'FAILED', label: '失敗' }
    ]);
    expect(samplingService.getBatches).toHaveBeenCalledWith({
      role: 'SYSTEM_ADMIN',
      status: '',
      projectId: undefined,
      batchId: undefined,
      page: 0,
      size: 20
    });
  });

  it('uses the cross-cluster route contract for 查看案件', () => {
    fixture.detectChanges();

    component.onViewCases('BATCH-FAILED-001');

    expect(router.navigate).toHaveBeenCalledWith(['/cases'], {
      queryParams: { batchId: 'BATCH-FAILED-001' }
    });
  });

  it('renders batch detail as an explicit deferred state instead of a no-op stub', () => {
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('批次詳情已正式 deferred，原因為 UI spec 尚未定。');
    expect(router.navigate).not.toHaveBeenCalledWith(['/sampling/batches', 'BATCH-FAILED-001']);
  });

  it('persists role tab changes through query params before reloading the list', () => {
    fixture.detectChanges();

    component.onTabChange('FAILED');

    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: activatedRoute as unknown as ActivatedRoute,
      queryParams: { tab: 'FAILED' },
      queryParamsHandling: 'merge'
    });
    expect(component.uiState.status).toBe('FAILED');
  });
});
