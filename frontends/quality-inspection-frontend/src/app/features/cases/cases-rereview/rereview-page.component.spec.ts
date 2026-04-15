import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { ReviewCommandBarComponent } from '../components/review-command-bar/review-command-bar.component';
import { ReviewFormSummaryStripComponent } from '../components/review-form-summary-strip/review-form-summary-strip.component';
import { ReviewVersionSummaryRegionComponent } from '../components/review-version-summary-region/review-version-summary-region.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { RereviewContext, RereviewPageComponent } from './rereview-page.component';

describe('RereviewPageComponent', () => {
  let fixture: ComponentFixture<RereviewPageComponent>;
  let component: RereviewPageComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        RereviewPageComponent,
        CaseSummaryHeaderComponent,
        ProjectionNoticeBannerComponent,
        StatusOrBlockedBannerComponent,
        ModeIndicatorStripComponent,
        ReviewFormSummaryStripComponent,
        ReviewCommandBarComponent,
        ReviewVersionSummaryRegionComponent
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ caseId: 'CASE-013' })
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RereviewPageComponent);
    component = fixture.componentInstance;
  });

  function buildContext(overrides: Partial<RereviewContext> = {}): RereviewContext {
    return {
      caseId: 'CASE-013',
      caseTitle: '複審測試骨架',
      status: 'PENDING_REREVIEW',
      stageLabel: '待複審',
      blockedReason: null,
      hasDraft: false,
      hasSubmittedBase: false,
      isClosedRevisionException: false,
      initialSubmissionId: 'SUB-INITIAL-001',
      initialReviewerName: 'Dr. Initial',
      initialTotalScore: '79',
      finalDecisionSummary: '最終結論 placeholder',
      ...overrides
    };
  }

  function render(context: RereviewContext): void {
    fixture.detectChanges();
    component.applyContext(context);
    fixture.detectChanges();
  }

  it('covers NEW baseline with C18/C19 skeleton and rereview form placeholder', () => {
    render(buildContext());

    expect(component.mode).toBe('NEW');
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-initial-summary-reference"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-final-decision-summary"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-form-placeholder"]')).not.toBeNull();
  });

  it('covers EDIT_DRAFT baseline', () => {
    render(buildContext({ hasDraft: true, status: 'REREVIEW_IN_PROGRESS' }));

    expect(component.mode).toBe('EDIT_DRAFT');
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).not.toBeNull();
  });

  it('covers REVISION baseline and keeps C17 visible', () => {
    render(buildContext({
      hasSubmittedBase: true,
      basedOnVersion: { submissionId: 'SUB-REREVIEW-001', version: 1, submittedAt: '2026-04-15T11:00:00+08:00' }
    }));

    expect(component.mode).toBe('REVISION');
    expect(fixture.nativeElement.querySelector('app-review-version-summary-region')).not.toBeNull();
  });

  it('covers CLOSED_REVISION_EXCEPTION baseline and shows the exception banner with C16 placeholder label', () => {
    render(buildContext({
      status: 'CLOSED',
      isClosedRevisionException: true,
      hasSubmittedBase: true,
      basedOnVersion: { submissionId: 'SUB-REREVIEW-002', version: 2 }
    }));

    expect(component.mode).toBe('CLOSED_REVISION_EXCEPTION');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rereview-c16-placeholder-label"]')).not.toBeNull();
  });

  it('covers READONLY_BLOCKED baseline and hides the command bar', () => {
    render(buildContext({ status: 'CANCELLED', blockedReason: 'CASE_ALREADY_CANCELLED' }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).toBeNull();
  });
});
