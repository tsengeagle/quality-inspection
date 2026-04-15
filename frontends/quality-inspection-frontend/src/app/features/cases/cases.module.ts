import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CasesRoutingModule } from './cases-routing.module';
import { CaseListPageComponent } from './cases-worklist/case-list-page.component';
import { CaseDetailPageComponent } from './case-detail/case-detail-page.component';
import { CaseAssignmentPageComponent } from './cases-assignment-command/case-assignment-page.component';
import { InitialReviewPageComponent } from './cases-initial-review/initial-review-page.component';
import { RereviewPageComponent } from './cases-rereview/rereview-page.component';
import { RevisionHistoryPageComponent } from './revision-history/revision-history-page.component';

// Wave 2 — S-series shared components (C01/C02/C03/C04/C15/C16/C17)
import { CaseSummaryHeaderComponent } from './components/case-summary-header/case-summary-header.component';
import { StatusOrBlockedBannerComponent } from './components/status-or-blocked-banner/status-or-blocked-banner.component';
import { ModeIndicatorStripComponent } from './components/mode-indicator-strip/mode-indicator-strip.component';
import { ProjectionNoticeBannerComponent } from './components/projection-notice-banner/projection-notice-banner.component';
import { ReviewFormSummaryStripComponent } from './components/review-form-summary-strip/review-form-summary-strip.component';
import { ReviewCommandBarComponent } from './components/review-command-bar/review-command-bar.component';
import { ReviewVersionSummaryRegionComponent } from './components/review-version-summary-region/review-version-summary-region.component';
// Wave 3 — C-01~C-04 feature components
import { CaseWorkQueueFilterBarComponent } from './cases-worklist/components/case-work-queue-filter-bar/case-work-queue-filter-bar.component';
import { CaseWorkQueueTableComponent } from './cases-worklist/components/case-work-queue-table/case-work-queue-table.component';
import { CaseRowActionSetComponent } from './cases-worklist/components/case-row-action-set/case-row-action-set.component';
import { CaseControlCenterNavComponent } from './case-detail/components/case-control-center-nav/case-control-center-nav.component';
import { InitialSummaryRegionComponent } from './case-detail/components/initial-summary-region/initial-summary-region.component';
import { ReviewerAssignmentInfoComponent } from './case-detail/components/reviewer-assignment-info/reviewer-assignment-info.component';
// Wave 3 — C-12 revision-history child components
import { RevisionTimelineComponent } from './revision-history/components/revision-timeline/revision-timeline.component';
import { RevisionCompareSummaryComponent } from './revision-history/components/revision-compare-summary/revision-compare-summary.component';
import { RevisionNavActionZoneComponent } from './revision-history/components/revision-nav-action-zone/revision-nav-action-zone.component';

@NgModule({
  declarations: [
    // Page components (Wave 1 — F-03)
    CaseListPageComponent,
    CaseDetailPageComponent,
    CaseAssignmentPageComponent,
    InitialReviewPageComponent,
    RereviewPageComponent,
    RevisionHistoryPageComponent,
    // Shared components within cases cluster (Wave 2 — S-01~S-07)
    CaseSummaryHeaderComponent,       // C01
    StatusOrBlockedBannerComponent,   // C02
    ModeIndicatorStripComponent,      // C03
    ProjectionNoticeBannerComponent,  // C04
    ReviewFormSummaryStripComponent,  // C15
    ReviewCommandBarComponent,        // C16
    ReviewVersionSummaryRegionComponent, // C17
    // Wave 3 — C-01: CaseListPage child components
    CaseWorkQueueFilterBarComponent,  // C05
    CaseWorkQueueTableComponent,      // C06
    CaseRowActionSetComponent,        // C07
    // Wave 3 — C-03: CaseDetailPage child components
    CaseControlCenterNavComponent,    // C08
    InitialSummaryRegionComponent,    // C09
    ReviewerAssignmentInfoComponent,  // C10
    // Wave 3 — C-12: RevisionHistoryPage child components
    RevisionTimelineComponent,        // C20
    RevisionCompareSummaryComponent,  // C21
    RevisionNavActionZoneComponent    // C22
  ],
  imports: [
    CommonModule,
    CasesRoutingModule
  ]
})
export class CasesModule { }
