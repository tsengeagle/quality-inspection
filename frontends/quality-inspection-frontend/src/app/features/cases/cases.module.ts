import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CasesRoutingModule } from './cases-routing.module';
import { CaseListPageComponent } from './cases-worklist/case-list-page.component';
import { CaseDetailPageComponent } from './case-detail/case-detail-page.component';
import { CaseAssignmentPageComponent } from './cases-assignment-command/case-assignment-page.component';
import { InitialReviewPageComponent } from './cases-initial-review/initial-review-page.component';
import { RereviewPageComponent } from './cases-rereview/rereview-page.component';
import { RevisionHistoryPageComponent } from './revision-history/revision-history-page.component';

@NgModule({
  declarations: [
    CaseListPageComponent,
    CaseDetailPageComponent,
    CaseAssignmentPageComponent,
    InitialReviewPageComponent,
    RereviewPageComponent,
    RevisionHistoryPageComponent
  ],
  imports: [
    CommonModule,
    CasesRoutingModule
  ]
})
export class CasesModule { }
