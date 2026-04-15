import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { CaseSummaryHeaderComponent } from '../components/case-summary-header/case-summary-header.component';
import { ModeIndicatorStripComponent } from '../components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from '../components/projection-notice-banner/projection-notice-banner.component';
import { ReviewCommandBarComponent } from '../components/review-command-bar/review-command-bar.component';
import { ReviewFormSummaryStripComponent } from '../components/review-form-summary-strip/review-form-summary-strip.component';
import { ReviewVersionSummaryRegionComponent } from '../components/review-version-summary-region/review-version-summary-region.component';
import { StatusOrBlockedBannerComponent } from '../components/status-or-blocked-banner/status-or-blocked-banner.component';
import { InitialReviewContext, InitialReviewPageComponent } from './initial-review-page.component';

describe('InitialReviewPageComponent', () => {
  let fixture: ComponentFixture<InitialReviewPageComponent>;
  let component: InitialReviewPageComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        InitialReviewPageComponent,
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
              paramMap: convertToParamMap({ caseId: 'CASE-012' })
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InitialReviewPageComponent);
    component = fixture.componentInstance;
  });

  function buildContext(overrides: Partial<InitialReviewContext> = {}): InitialReviewContext {
    return {
      caseId: 'CASE-012',
      caseTitle: '初審測試骨架',
      status: 'PENDING_INITIAL_REVIEW',
      stageLabel: '待初審',
      blockedReason: null,
      hasDraft: false,
      hasSubmittedBase: false,
      ...overrides
    };
  }

  function render(context: InitialReviewContext): void {
    fixture.detectChanges();
    component.applyContext(context);
    fixture.detectChanges();
  }

  it('covers NEW baseline and renders C15/C16 shared structure', () => {
    render(buildContext());

    expect(component.mode).toBe('NEW');
    expect(fixture.nativeElement.querySelector('app-review-form-summary-strip')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-form-placeholder"]')).not.toBeNull();
  });

  it('covers EDIT_DRAFT baseline and keeps the placeholder regions visible', () => {
    render(buildContext({ hasDraft: true, status: 'INITIAL_REVIEW_IN_PROGRESS' }));

    expect(component.mode).toBe('EDIT_DRAFT');
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-commentary-placeholder"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-attachment-placeholder"]')).not.toBeNull();
  });

  it('covers REVISION baseline, append-only copy, and C17 visibility', () => {
    render(buildContext({
      hasSubmittedBase: true,
      basedOnVersion: { submissionId: 'SUB-INITIAL-002', version: 2, submittedAt: '2026-04-15T09:00:00+08:00' }
    }));

    expect(component.mode).toBe('REVISION');
    expect(fixture.nativeElement.querySelector('app-review-version-summary-region')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('系統將建立新 revision，不會覆寫既有提交。');
  });

  it('covers READONLY_BLOCKED baseline and shows form restriction grammar', () => {
    render(buildContext({ status: 'CLOSED', blockedReason: 'CASE_ALREADY_CLOSED' }));

    expect(component.mode).toBe('READONLY_BLOCKED');
    expect(fixture.nativeElement.querySelector('app-status-or-blocked-banner')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="initial-review-readonly-guard"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-review-command-bar')).toBeNull();
  });
});
