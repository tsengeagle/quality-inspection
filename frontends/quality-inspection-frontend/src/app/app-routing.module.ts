import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { RoleGuard } from './guards/role.guard';
import { ScopeGuard } from './guards/scope.guard';
import { ProjectionGuard } from './guards/projection.guard';
import { DemoEntryPageComponent } from './demo/demo-entry-page.component';

export const appRoutes: Routes = [
  // Feature routes will be added here as lazy-loaded modules.
  // See docs/05-ui/ui-information-architecture.md for the page hierarchy.
  { path: '', redirectTo: 'demo', pathMatch: 'full' },
  { path: 'demo', component: DemoEntryPageComponent },
  {
    path: 'sampling',
    canActivate: [RoleGuard, ScopeGuard, ProjectionGuard],
    canActivateChild: [RoleGuard, ScopeGuard, ProjectionGuard],
    loadChildren: () =>
      import('./features/sampling/sampling.module').then(m => m.SamplingModule)
  },
  {
    path: 'cases',
    canActivate: [RoleGuard, ScopeGuard, ProjectionGuard],
    canActivateChild: [RoleGuard, ScopeGuard, ProjectionGuard],
    loadChildren: () =>
      import('./features/cases/cases.module').then(m => m.CasesModule)
  },
  {
    path: 'reports',
    canActivate: [RoleGuard, ScopeGuard, ProjectionGuard],
    canActivateChild: [RoleGuard, ScopeGuard, ProjectionGuard],
    loadChildren: () =>
      import('./features/reports/reports.module').then(m => m.ReportsModule)
  },
  { path: '**', redirectTo: 'demo' }
];

@NgModule({
  imports: [RouterModule.forRoot(appRoutes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
