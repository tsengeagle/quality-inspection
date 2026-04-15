import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

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
        { provide: CasesService, useValue: casesService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CaseDetailPageComponent);
    component = fixture.componentInstance;
  });

  function render(detail: CaseDetailViewModel | null): void {
    casesService.getCaseDetail.and.returnValue(of(detail));
    fixture.detectChanges();
  }

  it('resolves the metadata-only control center and hides command CTAs', () => {
    render({
      caseId: 'CASE-010',
      caseTitle: '案件詳情',
      patientId: 'P001',
      status: 'INITIAL_REVIEW_COMPLETED',
      stageLabel: '初審完成',
      role: 'SYSTEM_ADMIN',
      projectionType: 'METADATA_ONLY',
      isOwner: false,
      lifecycleLatestStage: 'INITIAL_REVIEW_COMPLETED',
      latestRevisionSummary: '最後一次修正摘要'
    });

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
    render({
      caseId: 'CASE-010',
      caseTitle: '案件詳情',
      patientId: 'P001',
      status: 'PENDING_REREVIEW',
      stageLabel: '待複審',
      role: 'HOSPITAL_MANAGER',
      projectionType: 'FULL',
      isOwner: false,
      lifecycleLatestStage: 'PENDING_REREVIEW',
      latestRevisionSummary: '待複審摘要'
    });

    expect(component.mode).toBe('FULL_CONTROL_CENTER');

    const tags = Array.from((fixture.nativeElement as HTMLElement).children)
      .map(node => node.tagName.toLowerCase());
    expect(tags.slice(0, 2)).toEqual(['app-case-summary-header', 'app-mode-indicator-strip']);
    expect(fixture.nativeElement.textContent).toContain('案件指派');
    expect(fixture.nativeElement.textContent).toContain('Revision 歷程');
  });

  it('resolves role-limited access for owned reviewer cases', () => {
    render({
      caseId: 'CASE-010',
      caseTitle: '案件詳情',
      patientId: 'P001',
      status: 'PENDING_INITIAL_REVIEW',
      stageLabel: '待初審',
      role: 'REVIEWER',
      projectionType: 'FULL',
      isOwner: true,
      lifecycleLatestStage: 'PENDING_INITIAL_REVIEW',
      latestRevisionSummary: ''
    });

    expect(component.mode).toBe('ROLE_LIMITED');
    expect(fixture.nativeElement.textContent).toContain('初審作業');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-initial-summary-region')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('app-rereview-summary-region')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('案件指派');
  });

  it('shows rereview summary for role-limited rereviewer cases only', () => {
    render({
      caseId: 'CASE-010',
      caseTitle: '案件詳情',
      patientId: 'P001',
      status: 'REREVIEW_IN_PROGRESS',
      stageLabel: '複審中',
      role: 'REREVIEWER',
      projectionType: 'FULL',
      isOwner: true,
      lifecycleLatestStage: 'REREVIEW_IN_PROGRESS',
      latestRevisionSummary: '複審摘要'
    });

    expect(component.mode).toBe('ROLE_LIMITED');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-initial-summary-region')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('app-rereview-summary-region')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('複審作業');
    expect(fixture.nativeElement.textContent).not.toContain('案件指派');
  });

  it('resolves terminal mode and shows the blocked banner before the mode strip', () => {
    render({
      caseId: 'CASE-010',
      caseTitle: '案件詳情',
      patientId: 'P001',
      status: 'CLOSED',
      stageLabel: '已結案',
      role: 'HOSPITAL_MANAGER',
      projectionType: 'FULL',
      isOwner: false
    });

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
