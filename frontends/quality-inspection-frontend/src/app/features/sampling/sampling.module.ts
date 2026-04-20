import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';

import { SamplingRoutingModule } from './sampling-routing.module';
import { SamplingExecutePageComponent } from './sampling-execute/sampling-execute-page.component';
import { SamplingBatchListPageComponent } from './batch-list/sampling-batch-list-page.component';
// SP-02: BatchList child components
import { BatchStatusTabComponent } from './batch-list/components/batch-status-tab/batch-status-tab.component';
import { BatchFilterBarComponent } from './batch-list/components/batch-filter-bar/batch-filter-bar.component';
import { BatchTableComponent } from './batch-list/components/batch-table/batch-table.component';
import { BatchRowActionsComponent } from './batch-list/components/batch-row-actions/batch-row-actions.component';
// SP-03: RetryConfirmModal
import { RetryConfirmModalComponent } from './batch-list/components/retry-confirm-modal/retry-confirm-modal.component';
// SP-05~SP-07: SamplingExecute child components
import { SamplingRuleSummaryComponent } from './sampling-execute/components/sampling-rule-summary/sampling-rule-summary.component';
import { SamplingPreviewStatsComponent } from './sampling-execute/components/sampling-preview-stats/sampling-preview-stats.component';
import { SamplingPostExecuteRegionComponent } from './sampling-execute/components/sampling-post-execute-region/sampling-post-execute-region.component';
import { SamplingBlockedBannerComponent } from './sampling-execute/components/sampling-blocked-banner/sampling-blocked-banner.component';
import { SamplingExecuteConfirmModalComponent } from './sampling-execute/components/sampling-execute-confirm-modal/sampling-execute-confirm-modal.component';

@NgModule({
  declarations: [
    SamplingExecutePageComponent,
    SamplingBatchListPageComponent,
    // SP-02
    BatchStatusTabComponent,
    BatchFilterBarComponent,
    BatchTableComponent,
    BatchRowActionsComponent,
    // SP-03
    RetryConfirmModalComponent,
    // SP-05~SP-07
    SamplingRuleSummaryComponent,
    SamplingPreviewStatsComponent,
    SamplingPostExecuteRegionComponent,
    SamplingBlockedBannerComponent,
    SamplingExecuteConfirmModalComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    SamplingRoutingModule
  ]
})
export class SamplingModule { }

