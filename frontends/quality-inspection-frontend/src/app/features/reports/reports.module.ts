import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ReportExportPageComponent } from './report-export/report-export-page.component';
import { ReportsRoutingModule } from './reports-routing.module';

@NgModule({
  declarations: [ReportExportPageComponent],
  imports: [CommonModule, FormsModule, ReportsRoutingModule]
})
export class ReportsModule { }
