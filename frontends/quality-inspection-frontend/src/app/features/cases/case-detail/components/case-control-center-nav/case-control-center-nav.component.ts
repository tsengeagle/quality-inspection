import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CaseDetailMode } from '../../case-detail-page.component';

/** C08 — CaseControlCenterNav (UI-010)
 *  依 mode 決定可顯示的 navigation actions。 */
@Component({
  selector: 'app-case-control-center-nav',
  template: `
    <nav class="control-center-nav">
      <!-- 永遠顯示：查看初審作業 -->
      <button class="nav-btn" (click)="navigate.emit('initial-review')">初審作業</button>

      <!-- 複審相關（非 READONLY_TERMINAL 才顯示） -->
      <button
        *ngIf="mode !== 'READONLY_TERMINAL'"
        class="nav-btn"
        (click)="navigate.emit('rereview')">
        複審作業
      </button>

      <!-- 指派（FULL_CONTROL_CENTER 才顯示） -->
      <button
        *ngIf="mode === 'FULL_CONTROL_CENTER'"
        class="nav-btn"
        (click)="navigate.emit('assign')">
        案件指派
      </button>

      <!-- 歷程查看 -->
      <button class="nav-btn" (click)="navigate.emit('revision-history')">Revision 歷程</button>
    </nav>
  `,
  styles: [`
    .control-center-nav { display: flex; gap: 8px; padding: 12px 16px; border-bottom: 1px solid #e0e0e0; }
    .nav-btn { padding: 6px 16px; border: 1px solid #1565c0; border-radius: 4px; cursor: pointer; background: #fff; color: #1565c0; }
  `]
})
export class CaseControlCenterNavComponent {
  @Input() caseId = '';
  @Input() mode: CaseDetailMode = 'READONLY_TERMINAL';
  @Output() navigate = new EventEmitter<string>();
}
