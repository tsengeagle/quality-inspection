import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SamplingBatchListPageComponent } from './batch-list/sampling-batch-list-page.component';
import { SamplingExecutePageComponent } from './sampling-execute/sampling-execute-page.component';

// CRITICAL: 'batches' literal MUST appear before ':projectId' wildcard.
// See docs/06-frontend/sampling-route-module-map-addendum.md §4.1.3
const routes: Routes = [
  { path: 'batches',    component: SamplingBatchListPageComponent },  // UI-007
  { path: ':projectId', component: SamplingExecutePageComponent },    // UI-006
  { path: '',           redirectTo: 'batches', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SamplingRoutingModule { }
