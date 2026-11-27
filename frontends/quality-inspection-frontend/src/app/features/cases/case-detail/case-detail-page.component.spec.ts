import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { ActorContextService } from '@core/actor-context/actor-context.service';
import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { CaseDetailViewModel, CaseMetadataViewModel, CasesService } from '../services/cases.service';
import { casesRoutes } from '../cases-routing.module';
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
    casesService = jasmine.createSpyObj<CasesService>('CasesService', ['getCaseDetailOrMetadata']);

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

  function render(detail: CaseDetailViewModel | CaseMetadataViewModel): void {
    casesService.getCaseDetailOrMetadata.and.returnValue(of(detail));
    fixture.detectChanges();
  }

  it('resolves the metadata-only control center from the seven-field metadata view model (D-036、D-094)', () => {
    render({
      projectionType: 'METADATA_ONLY',
      caseId: 'CASE-010',
      status: 'REREVIEW_IN_PROGRESS',
      projectName: '品質審查專案',
      hospitalId: 'H001',
      reviewerId: 'reviewer-1',
      rereviewerId: null,
      updatedAt: '2026-11-19T09:00:00Z'
    });

    expect(component.mode).toBe('METADATA_CONTROL_CENTER');
    expect(component.showProjectionNotice).toBeTrue();
    expect(component.showBanner).toBeFalse();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('app-status-or-blocked-banner')).toBeNull();
    expect(element.textContent).toContain('控制中心（Metadata）');
    // UI-010 §5.5：頁首的狀態來自 caseStatus
    expect(element.querySelector('app-case-summary-header')?.textContent).toContain('REREVIEW_IN_PROGRESS');
    // 案件 metadata 區塊：七欄裡頁首以外的五欄
    const block = element.querySelector('[data-testid="case-metadata-block"]');
    expect(block).not.toBeNull();
    expect(element.querySelector('[data-testid="case-metadata-project-name"]')?.textContent).toContain('品質審查專案');
    expect(element.querySelector('[data-testid="case-metadata-hospital-id"]')?.textContent).toContain('H001');
    expect(element.querySelector('[data-testid="case-metadata-reviewer-id"]')?.textContent).toContain('reviewer-1');
    expect(element.querySelector('[data-testid="case-metadata-rereviewer-id"]')?.textContent).toContain('—');
    expect(element.querySelector('[data-testid="case-metadata-updated-at"]')?.textContent).toContain('2026-11-19 17:00');
    // 摘要區不顯示；生命週期與「Revision 歷程」保留
    expect(element.querySelector('app-initial-summary-region')).toBeNull();
    expect(element.querySelector('app-rereview-summary-region')).toBeNull();
    expect(element.querySelector('app-lifecycle-progress-region')).not.toBeNull();
    expect(element.textContent).toContain('Revision 歷程');
    expect(element.textContent).not.toContain('初審作業');
    expect(element.textContent).not.toContain('複審作業');
    expect(element.textContent).not.toContain('案件指派');
  });

  it('navigates from the Revision 歷程 button to a route that exists', () => {
    render(buildDetail({ status: 'PENDING_INITIAL_REVIEW', actorRole: 'HOSPITAL_MANAGER' }));
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    const button = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.control-center-nav button'))
      .find(item => item.textContent?.includes('Revision 歷程')) as HTMLButtonElement;

    button.click();

    expect(router.navigate).toHaveBeenCalledOnceWith(['/cases', 'CASE-010', 'revisions']);
    // 導覽的目的地必須是路由表裡真的有的路徑（原本寫成 revision-history，被導到預設頁）
    expect(casesRoutes.some(route => route.path === ':caseId/revisions')).toBeTrue();
  });

  it('does not show the metadata block for a full case detail', () => {
    render(buildDetail({ status: 'PENDING_INITIAL_REVIEW', actorRole: 'HOSPITAL_MANAGER' }));

    expect(component.mode).toBe('FULL_CONTROL_CENTER');
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="case-metadata-block"]')).toBeNull();
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
