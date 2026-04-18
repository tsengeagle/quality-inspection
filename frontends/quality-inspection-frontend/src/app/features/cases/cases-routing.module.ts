import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { CaseListPageComponent } from './cases-worklist/case-list-page.component';
import { CaseDetailPageComponent } from './case-detail/case-detail-page.component';
import { CaseAssignmentPageComponent } from './cases-assignment-command/case-assignment-page.component';
import { InitialReviewPageComponent } from './cases-initial-review/initial-review-page.component';
import { RereviewPageComponent } from './cases-rereview/rereview-page.component';
import { RevisionHistoryPageComponent } from './revision-history/revision-history-page.component';

export const casesRoutes: Routes = [
  { path: '',                       component: CaseListPageComponent },       // UI-009
  { path: ':caseId',                component: CaseDetailPageComponent },     // UI-010
  {
    path: ':caseId/assign',
    component: CaseAssignmentPageComponent,
    data: {
      roles: ['HOSPITAL_MANAGER'],
      requiresHospitalScope: true
    }
  }, // UI-011
  { path: ':caseId/initial-review', component: InitialReviewPageComponent },  // UI-012
  { path: ':caseId/rereview',       component: RereviewPageComponent },       // UI-013
  { path: ':caseId/revisions',      component: RevisionHistoryPageComponent } // UI-014
];

@NgModule({
  imports: [RouterModule.forChild(casesRoutes)],
  exports: [RouterModule]
})
export class CasesRoutingModule { }
