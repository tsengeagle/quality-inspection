import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Params, Router } from '@angular/router';
import { of } from 'rxjs';

import { SamplingService } from '../services/sampling.service';
import { SamplingBatchListPageComponent } from './sampling-batch-list-page.component';

class ActivatedRouteSnapshotStub {
  snapshot = {
    queryParamMap: convertToParamMap({})
  };

  setQueryParams(params: Params): void {
    this.snapshot = {
      queryParamMap: convertToParamMap(params)
    };
  }
}

describe('SamplingBatchListPageComponent', () => {
  let fixture: ComponentFixture<SamplingBatchListPageComponent>;
  let component: SamplingBatchListPageComponent;
  let activatedRoute: ActivatedRouteSnapshotStub;
  let router: jasmine.SpyObj<Router>;
  let samplingService: jasmine.SpyObj<SamplingService>;

  beforeEach(async () => {
    activatedRoute = new ActivatedRouteSnapshotStub();
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

  it('resolves the SYSTEM_ADMIN baseline into metadata-only tab availability', () => {
    component.role = 'SYSTEM_ADMIN';

    fixture.detectChanges();

    expect(component.uiState.tab).toBe('ALL_BATCHES');
    expect(component.availableTabs).toEqual([
      { value: 'ALL_BATCHES', label: '所有批次' }
    ]);
    expect(samplingService.getBatches).toHaveBeenCalledWith({
      role: 'SYSTEM_ADMIN',
      status: ''
    });
  });

  it('persists sampling tab changes through query params before reloading the list', () => {
    fixture.detectChanges();

    component.onTabChange('ALL_BATCHES');

    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: activatedRoute as unknown as ActivatedRoute,
      queryParams: { tab: 'ALL_BATCHES' },
      queryParamsHandling: 'merge'
    });
    expect(samplingService.getBatches).toHaveBeenCalledTimes(2);
  });
});