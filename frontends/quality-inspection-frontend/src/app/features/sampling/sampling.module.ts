import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SamplingRoutingModule } from './sampling-routing.module';
import { SamplingExecutePageComponent } from './sampling-execute/sampling-execute-page.component';
import { SamplingBatchListPageComponent } from './batch-list/sampling-batch-list-page.component';

@NgModule({
  declarations: [
    SamplingExecutePageComponent,
    SamplingBatchListPageComponent
  ],
  imports: [
    CommonModule,
    SamplingRoutingModule
  ]
})
export class SamplingModule { }
