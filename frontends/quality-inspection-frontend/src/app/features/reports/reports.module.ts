import { CommonModule } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ReportDashboardPageComponent } from './report-dashboard/report-dashboard-page.component';
import { ReportExportPageComponent } from './report-export/report-export-page.component';
import { ReportsRoutingModule } from './reports-routing.module';

@NgModule({
  declarations: [ReportDashboardPageComponent, ReportExportPageComponent],
  imports: [CommonModule, FormsModule, ReportsRoutingModule],
  // system-container／system-block 是 shell 提供的版面元素（GOV-BND-001），不是 Angular 元件。
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ReportsModule { }
