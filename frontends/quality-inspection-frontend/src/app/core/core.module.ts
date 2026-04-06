import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

/**
 * CoreModule — singleton services, interceptors, guards.
 * Imported once in AppModule.
 */
@NgModule({
  imports: [
    CommonModule,
    HttpClientModule
  ]
})
export class CoreModule { }
