import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { SamplingBatchListPageComponent } from './batch-list/sampling-batch-list-page.component';
import { SamplingExecutePageComponent } from './sampling-execute/sampling-execute-page.component';

// CRITICAL: 'batches' literal MUST appear before ':projectId' wildcard.
// See docs/06-frontend/sampling-route-module-map-addendum.md §4.1.3
export const samplingRoutes: Routes = [
  {
    path: 'batches',
    component: SamplingBatchListPageComponent,
    data: {
      roles: ['HOSPITAL_MANAGER', 'AUDITOR', 'SYSTEM_ADMIN']
    }
  },  // UI-007
  {
    path: ':projectId',
    component: SamplingExecutePageComponent,
    data: {
      roles: ['HOSPITAL_MANAGER'],
      requiresHospitalScope: true
    }
  },    // UI-006
  { path: '',           redirectTo: 'batches', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forChild(samplingRoutes)],
  exports: [RouterModule]
})
export class SamplingRoutingModule { }
