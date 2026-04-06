import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  // Feature routes will be added here as lazy-loaded modules.
  // See docs/05-ui/ui-information-architecture.md for the page hierarchy.
  { path: '', redirectTo: 'shell', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
