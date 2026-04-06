import { Component } from '@angular/core';

/**
 * Shell component — layout, navigation, route container.
 * In standalone mode, this acts as the local shell.
 * In integrated mode, the organization MFE shell wraps this application.
 */
@Component({
  selector: 'app-shell',
  template: `
    <header>
      <h1>品質審查系統</h1>
    </header>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styles: []
})
export class ShellComponent { }
