import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { SamplingService } from '../services/sampling.service';
import { SamplingBatchListPageComponent } from './sampling-batch-list-page.component';

class ActivatedRouteStub {
  private readonly queryParamsSubject = new BehaviorSubject<Params>({});

  readonly queryParams = this.queryParamsSubject.asObservable();

  setQueryParams(params: Params): void {
    this.queryParamsSubject.next(params);
  }
}

describe('SamplingBatchListPageComponent', () => {
  let fixture: ComponentFixture<SamplingBatchListPageComponent>;
  let component: SamplingBatchListPageComponent;
  let activatedRoute: ActivatedRouteStub;
  let router: jasmine.SpyObj<Router>;
  let samplingService: jasmine.SpyObj<SamplingService>;

  beforeEach(async () => {
    activatedRoute = new ActivatedRouteStub();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    samplingService = jasmine.createSpyObj<SamplingService>('SamplingService', ['getBatches']);
    samplingService.getBatches.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [SamplingBatchListPageComponent],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: Router, useValue: router },
        { provide: SamplingService, useValue: samplingService }
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
    component.role = 'SYSTEM_ADMIN';
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

  it('keeps 查看詳情 as a no-op stub without navigation', () => {
    fixture.detectChanges();

    component.onViewDetailStub('BATCH-FAILED-001');

    expect(router.navigate).not.toHaveBeenCalled();
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
