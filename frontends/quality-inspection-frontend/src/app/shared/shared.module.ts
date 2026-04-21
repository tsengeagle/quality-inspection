import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CanAccessDirective } from './access/can-access.directive';
import { HasPermissionDirective } from './access/has-permission.directive';
import { HasRoleDirective } from './access/has-role.directive';

/**
 * SharedModule — reusable UI components, pipes, directives.
 * portal51-lib is the shared UI library baseline and wrapper/style priority reference.
 * Component-level usage detail pending confirmation.
 */
@NgModule({
  imports: [
    CommonModule
  ],
  declarations: [
    HasRoleDirective,
    HasPermissionDirective,
    CanAccessDirective
  ],
  exports: [
    CommonModule,
    HasRoleDirective,
    HasPermissionDirective,
    CanAccessDirective
  ]
})
export class SharedModule { }
