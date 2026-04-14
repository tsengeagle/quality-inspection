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
    ReviewVersionSummaryRegionComponent // C17
  ],
  imports: [
    CommonModule,
    CasesRoutingModule
  ]
})
export class CasesModule { }
