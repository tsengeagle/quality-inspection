import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ReportExportPageComponent } from './report-export/report-export-page.component';

// UI-017：docs/05-ui/ui-page-spec-ui017報表匯出頁.md §1
export const reportsRoutes: Routes = [
  {
    path: 'export',
    component: ReportExportPageComponent,
    data: {
      roles: ['HOSPITAL_MANAGER', 'AUDITOR', 'SYSTEM_ADMIN']
    }
  },
  { path: '', redirectTo: 'export', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forChild(reportsRoutes)],
  exports: [RouterModule]
})
export class ReportsRoutingModule { }
