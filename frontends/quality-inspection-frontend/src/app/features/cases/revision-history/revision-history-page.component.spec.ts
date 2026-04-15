import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { CasesService, RevisionHistoryViewModel } from '../services/cases.service';
import { RevisionHistoryPageComponent } from './revision-history-page.component';
import { RevisionCompareSummaryComponent } from './components/revision-compare-summary/revision-compare-summary.component';
import { RevisionNavActionZoneComponent } from './components/revision-nav-action-zone/revision-nav-action-zone.component';
import { RevisionTimelineComponent } from './components/revision-timeline/revision-timeline.component';

function buildVm(overrides: Partial<RevisionHistoryViewModel> = {}): RevisionHistoryViewModel {
  return {
    caseId: 'CASE-014',
    status: 'REREVIEW_IN_PROGRESS',
    actorRole: 'REREVIEWER',
    projectionType: 'FULL',
    hasExceptionEntry: false,
    revisions: [
      {
        submissionId: 'SUB-1',
        stage: 'INITIAL',
        reviewerId: 'Dr. A',
        status: 'SUPERSEDED',
        revisionNo: 1,
        submittedAt: '2026-04-01T08:00:00Z',
        supersededBySubmissionId: 'SUB-2',
        isCurrentEffective: false
      },
      {
        submissionId: 'SUB-2',
        stage: 'REREVIEW',
        reviewerId: 'Dr. B',
        status: 'SUBMITTED',
        revisionNo: 2,
        submittedAt: '2026-04-02T08:00:00Z',
        isCurrentEffective: true
      }
    ],
    ...overrides
  };
}

describe('RevisionHistoryPageComponent', () => {
  let fixture: ComponentFixture<RevisionHistoryPageComponent>;
  let component: RevisionHistoryPageComponent;
  let casesService: jasmine.SpyObj<CasesService>;
  let router: jasmine.SpyObj<Router>;

  const activatedRouteStub = {
    snapshot: {
      paramMap: convertToParamMap({ caseId: 'CASE-014' }),
      queryParamMap: convertToParamMap({ stageTab: 'ALL' })
    }
  };

  beforeEach(async () => {
    activatedRouteStub.snapshot.queryParamMap = convertToParamMap({ stageTab: 'ALL' });
    casesService = jasmine.createSpyObj<CasesService>('CasesService', ['getRevisionHistory']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      declarations: [
        RevisionHistoryPageComponent,
        CaseSummaryHeaderComponent,
        StatusOrBlockedBannerComponent,
        ModeIndicatorStripComponent,
        ProjectionNoticeBannerComponent,
        RevisionTimelineComponent,
        RevisionCompareSummaryComponent,
        RevisionNavActionZoneComponent
      ],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: router },
        { provide: CasesService, useValue: casesService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RevisionHistoryPageComponent);
    component = fixture.componentInstance;
  });

  function render(vm: RevisionHistoryViewModel): void {
    casesService.getRevisionHistory.and.returnValue(of(vm));
    fixture.detectChanges();
  }

  it('resolves FULL_HISTORY and keeps compare summary in full mode', () => {
    render(buildVm());

    expect(component.mode).toBe('FULL_HISTORY');
    expect(component.stageTab).toBe('ALL');
    expect(component.showMetadataOnlyNotice).toBeFalse();
    expect(component.hasExceptionEntry).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('完整歷程');
    expect(fixture.nativeElement.textContent).toContain('Diff 比較區塊');
    expect(fixture.nativeElement.textContent).toContain('版本選擇互動尚未定案；目前採固定差異摘要檢視。');
  });

  it('resolves METADATA_HISTORY and shows the metadata compare notice', () => {
    render(buildVm({ projectionType: 'METADATA_ONLY', actorRole: 'SYSTEM_ADMIN' }));

    expect(component.mode).toBe('METADATA_HISTORY');
    expect(component.showMetadataOnlyNotice).toBeTrue();
    expect(component.hasExceptionEntry).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('詳細差異內容不適用於此視角。');
    expect(fixture.nativeElement.querySelector('app-revision-nav-action-zone')).toBeNull();
  });

  it('shows the exception entry only when all three conditions are met', () => {
    render(buildVm({ status: 'CLOSED', hasExceptionEntry: true }));

    expect(component.mode).toBe('READONLY_TERMINAL');
    expect(component.hasExceptionEntry).toBeTrue();
    expect(component.bannerTone).toBe('exception-amber');

    const button = fixture.nativeElement.querySelector('app-revision-nav-action-zone button');
    expect(button?.getAttribute('type')).toBe('button');
    expect(fixture.nativeElement.querySelector('button[type="submit"]')).toBeNull();
  });

  it('does not show the exception entry when the case is not CLOSED', () => {
    render(buildVm({ status: 'OPEN', hasExceptionEntry: true }));

    expect(component.hasExceptionEntry).toBeFalse();
  });

  it('does not show the exception entry when backend truth is false', () => {
    render(buildVm({ status: 'CLOSED', hasExceptionEntry: false }));

    expect(component.hasExceptionEntry).toBeFalse();
  });

  it('persists the stage tab through query params', () => {
    render(buildVm());

    component.onStageTabChange('REREVIEW_ONLY');

    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: activatedRouteStub as unknown as ActivatedRoute,
      queryParams: { stageTab: 'REREVIEW_ONLY' },
      queryParamsHandling: 'merge'
    });
  });

  it('defaults to INITIAL_ONLY when the actor is a reviewer and no query param overrides it', () => {
    activatedRouteStub.snapshot.queryParamMap = convertToParamMap({});
    render(buildVm({ actorRole: 'REVIEWER' }));

    expect(component.stageTab).toBe('INITIAL_ONLY');
  });

  it('defaults to ALL when the actor is a manager and no query param overrides it', () => {
    activatedRouteStub.snapshot.queryParamMap = convertToParamMap({});
    render(buildVm({ actorRole: 'HOSPITAL_MANAGER' }));

    expect(component.stageTab).toBe('ALL');
  });

  it('navigates to rereview from C22 as a navigate-only action', () => {
    render(buildVm({ status: 'CLOSED', hasExceptionEntry: true }));

    component.navigateToRereview();

    expect(router.navigate).toHaveBeenCalledWith(['/cases', 'CASE-014', 'rereview']);
  });
});
