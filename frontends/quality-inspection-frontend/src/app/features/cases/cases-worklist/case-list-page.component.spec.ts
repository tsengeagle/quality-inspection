import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { CasesService } from '../services/cases.service';
import { CaseListPageComponent } from './case-list-page.component';

class ActivatedRouteStub {
  private readonly queryParamsSubject = new BehaviorSubject<Params>({});

  readonly queryParams = this.queryParamsSubject.asObservable();

  setQueryParams(params: Params): void {
    this.queryParamsSubject.next(params);
  }
}

describe('CaseListPageComponent', () => {
  let fixture: ComponentFixture<CaseListPageComponent>;
  let component: CaseListPageComponent;
  let activatedRoute: ActivatedRouteStub;
  let router: jasmine.SpyObj<Router>;
  let casesService: jasmine.SpyObj<CasesService>;

  beforeEach(async () => {
    activatedRoute = new ActivatedRouteStub();
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    casesService = jasmine.createSpyObj<CasesService>('CasesService', ['getCases']);
    casesService.getCases.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [CaseListPageComponent],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: Router, useValue: router },
        { provide: CasesService, useValue: casesService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseListPageComponent);
    component = fixture.componentInstance;
  });

  it('resolves metadata-only mode from the container and hides business commands', () => {
    component.projectionType = 'METADATA_ONLY';
    activatedRoute.setQueryParams({});

    fixture.detectChanges();

    expect(component.uiState.tab).toBe('MY_QUEUE');
    expect(component.showBusinessCommands).toBeFalse();
    expect(component.showProjectionNotice).toBeTrue();
    expect(component.modeLabel).toBe('唯讀（metadata-only）');
    expect(casesService.getCases).toHaveBeenCalledWith(jasmine.objectContaining({
      role: 'REVIEWER',
      status: 'PENDING',
      page: 0,
      size: 20
    }));
  });

  it('uses the manager default tab and persists tab changes through query params', () => {
    component.role = 'MANAGER';
    activatedRoute.setQueryParams({});

    fixture.detectChanges();
    component.onTabChange('MY_QUEUE');

    expect(component.uiState.tab).toBe('MY_MANAGED_PROJECTS');
    expect(component.modeLabel).toBe('管理者視角');
    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: activatedRoute as unknown as ActivatedRoute,
      queryParams: { tab: 'MY_QUEUE', status: 'PENDING' },
      queryParamsHandling: 'merge'
    });
  });
});
