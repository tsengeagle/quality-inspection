import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * SharedModule — reusable UI components, pipes, directives.
 * portal51-lib is the shared UI library baseline and wrapper/style priority reference.
 * Component-level usage detail pending confirmation.
 */
@NgModule({
  imports: [
    CommonModule
  ],
  exports: [
    CommonModule
  ]
})
export class SharedModule { }
