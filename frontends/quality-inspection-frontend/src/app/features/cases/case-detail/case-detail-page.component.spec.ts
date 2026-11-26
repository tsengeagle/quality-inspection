import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { CaseDetailViewModel, CasesService } from '../services/cases.service';
import { CaseDetailPageComponent } from './case-detail-page.component';
import { CaseControlCenterNavComponent } from './components/case-control-center-nav/case-control-center-nav.component';
import { InitialSummaryRegionComponent } from './components/initial-summary-region/initial-summary-region.component';
import { LifecycleProgressRegionComponent } from './components/lifecycle-progress-region/lifecycle-progress-region.component';
import { RereviewSummaryRegionComponent } from './components/rereview-summary-region/rereview-summary-region.component';

function buildDetail(overrides: Partial<CaseDetailViewModel> = {}): CaseDetailViewModel {
  return {
    caseId: 'CASE-010',
    status: 'PENDING_INITIAL_REVIEW',
    projectionType: 'FULL',
    actorRole: 'REVIEWER',
    isOwner: false,
    batchId: 'BATCH-001',
    hospitalId: 'H001',
    reviewerId: 'reviewer-1',
    rereviewerId: 'rereviewer-1',
    medicalRecordType: 'OUTPATIENT',
    sourceCaseKey: 'SK-001',
    submissions: [],
    latestRereviewSubmissionId: null,
    ...overrides
  };
}

class ActorContextServiceStub {
  toCanonicalRole(role: string | null | undefined): string | null {
    switch (role) {
      case 'MANAGER':
        return 'HOSPITAL_MANAGER';
      case 'ADMIN':
      case 'SYSADMIN':
        return 'SYSTEM_ADMIN';
      case 'SYSTEM_ADMIN':
      case 'HOSPITAL_MANAGER':
      case 'AUDITOR':
      case 'REREVIEWER':
      case 'REVIEWER':
        return role;
      default:
        return null;
    }
  }

  isManagementRole(role: string | null | undefined): boolean {
    return this.toCanonicalRole(role) === 'HOSPITAL_MANAGER';
  }
}

describe('CaseDetailPageComponent', () => {
  let fixture: ComponentFixture<CaseDetailPageComponent>;
  let component: CaseDetailPageComponent;
  let casesService: jasmine.SpyObj<CasesService>;

  const activatedRouteStub = {
    snapshot: {
      paramMap: convertToParamMap({ caseId: 'CASE-010' })
    }
  };

  beforeEach(async () => {
    casesService = jasmine.createSpyObj<CasesService>('CasesService', ['getCaseDetail']);

    await TestBed.configureTestingModule({
      declarations: [
        CaseDetailPageComponent,
        CaseSummaryHeaderComponent,
        StatusOrBlockedBannerComponent,
        ModeIndicatorStripComponent,
        ProjectionNoticeBannerComponent,
        CaseControlCenterNavComponent,
        InitialSummaryRegionComponent,
        RereviewSummaryRegionComponent,
        LifecycleProgressRegionComponent
      ],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
        { provide: CasesService, useValue: casesService },
        { provide: ActorContextService, useValue: new ActorContextServiceStub() }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDetailPageComponent);
    component = fixture.componentInstance;
  });

  function render(detail: CaseDetailViewModel): void {
    casesService.getCaseDetail.and.returnValue(of(detail));
    fixture.detectChanges();
  }

  it('resolves the metadata-only control center and hides command CTAs', () => {
    render(buildDetail({
      status: 'INITIAL_REVIEW_COMPLETED',
      actorRole: 'SYSTEM_ADMIN',
      projectionType: 'METADATA_ONLY',
      isOwner: false,
      submissions: [
        {
          submissionId: 'SUB-001',
          stage: 'REREVIEW',
          reviewerId: 'rereviewer-1',
          status: 'SUBMITTED',
          revisionNo: 1,
          isCurrentEffective: true
        }
      ],
      latestRereviewSubmissionId: 'SUB-001'
    }));

    expect(component.mode).toBe('METADATA_CONTROL_CENTER');
    expect(component.showProjectionNotice).toBeTrue();
    expect(component.showBanner).toBeFalse();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('app-status-or-blocked-banner')).toBeNull();
    expect(element.textContent).toContain('控制中心（Metadata）');
    expect(element.querySelector('app-initial-summary-region')).toBeNull();
    expect(element.querySelector('app-rereview-summary-region')).not.toBeNull();
    expect(element.querySelector('app-lifecycle-progress-region')).not.toBeNull();
    expect(element.textContent).not.toContain('案件指派');
    expect(element.textContent).not.toContain('前往複審修正');
  });

  it('resolves the full control center and keeps the shared region order', () => {
    render(buildDetail({
      status: 'PENDING_REREVIEW',
      actorRole: 'HOSPITAL_MANAGER',
      projectionType: 'FULL',
      isOwner: false,
      submissions: [
        {
          submissionId: 'SUB-002',
          stage: 'REREVIEW',
          reviewerId: 'rereviewer-1',
          status: 'SUBMITTED',
          revisionNo: 2,
          isCurrentEffective: true
        }
      ],
      latestRereviewSubmissionId: 'SUB-002'
    }));

    expect(component.mode).toBe('FULL_CONTROL_CENTER');

    const tags = Array.from((fixture.nativeElement as HTMLElement).children)
      .map(node => node.tagName.toLowerCase());
    expect(tags.slice(0, 2)).toEqual(['app-case-summary-header', 'app-mode-indicator-strip']);
    expect(fixture.nativeElement.textContent).toContain('案件指派');
    expect(fixture.nativeElement.textContent).toContain('Revision 歷程');
  });

  it('keeps legacy manager aliases scoped to foundation canonicalization only', () => {
    render(buildDetail({
      status: 'PENDING_REREVIEW',
      actorRole: 'MANAGER',
      projectionType: 'FULL',
      isOwner: false
    }));

    expect(component.mode).toBe('FULL_CONTROL_CENTER');
  });

  it('resolves role-limited access for owned reviewer cases', () => {
    render(buildDetail({
      status: 'PENDING_INITIAL_REVIEW',
      actorRole: 'REVIEWER',
      projectionType: 'FULL',
      isOwner: true,
      submissions: [
        {
          submissionId: 'SUB-003',
          stage: 'INITIAL',
          reviewerId: 'reviewer-1',
          status: 'SUBMITTED',
          revisionNo: 1,
          isCurrentEffective: true
        }
      ]
    }));

    expect(component.mode).toBe('ROLE_LIMITED');
    expect(fixture.nativeElement.textContent).toContain('初審作業');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-initial-summary-region')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('app-rereview-summary-region')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('案件指派');
  });

  it('summarises a case without a rereview submission by its final score only (D-084 第 9 點)', () => {
    render(buildDetail({ status: 'CLOSED', finalScore: 88 }));
    expect(component.rereviewSummary).toBe('FINAL / 88');

    component.caseDetail = buildDetail({ status: 'CLOSED', finalScore: 0 });
    expect(component.rereviewSummary).toBe('FINAL / 0');

    component.caseDetail = buildDetail({ status: 'PENDING_INITIAL_REVIEW' });
    expect(component.rereviewSummary).toBeUndefined();
  });

  it('shows rereview summary for role-limited rereviewer cases only', () => {
    render(buildDetail({
      status: 'REREVIEW_IN_PROGRESS',
      actorRole: 'REREVIEWER',
      projectionType: 'FULL',
      isOwner: true,
      submissions: [
        {
          submissionId: 'SUB-004',
          stage: 'INITIAL',
          reviewerId: 'reviewer-1',
          status: 'SUBMITTED',
          revisionNo: 1,
          isCurrentEffective: false
        },
        {
          submissionId: 'SUB-005',
          stage: 'REREVIEW',
          reviewerId: 'rereviewer-1',
          status: 'SUBMITTED',
          revisionNo: 1,
          isCurrentEffective: true
        }
      ],
      latestRereviewSubmissionId: 'SUB-005'
    }));

    expect(component.mode).toBe('ROLE_LIMITED');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-initial-summary-region')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('app-rereview-summary-region')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('複審作業');
    expect(fixture.nativeElement.textContent).not.toContain('案件指派');
  });

  it('resolves terminal mode and shows the blocked banner before the mode strip', () => {
    render(buildDetail({
      status: 'CLOSED',
      actorRole: 'HOSPITAL_MANAGER',
      projectionType: 'FULL',
      isOwner: false
    }));

    expect(component.mode).toBe('READONLY_TERMINAL');
    expect(component.showBanner).toBeTrue();

    const tags = Array.from((fixture.nativeElement as HTMLElement).children)
      .map(node => node.tagName.toLowerCase());
    expect(tags.slice(0, 3)).toEqual([
      'app-case-summary-header',
      'app-status-or-blocked-banner',
      'app-mode-indicator-strip'
    ]);
    expect(fixture.nativeElement.textContent).not.toContain('案件指派');
    expect(fixture.nativeElement.textContent).not.toContain('前往複審修正');
  });
});
