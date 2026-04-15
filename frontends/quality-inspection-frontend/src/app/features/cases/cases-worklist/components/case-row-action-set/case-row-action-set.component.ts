import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';

/** C07 — CaseRowActionSet (UI-009)
 *  依 (role, caseState, scope) 決定 visible navigate actions。
 *  metadata-only business commands 必須完全 hidden（非 disabled）。 */
@Component({
  selector: 'app-case-row-action-set',
  template: `
    <div class="row-actions">
      <!-- [查看] — 永遠顯示 -->
      <button class="action-btn" (click)="navigateTo(['cases', caseId])">查看</button>

      <!-- Business commands — metadata-only 時 hidden（完全不渲染）  -->
      <ng-container *ngIf="showBusinessCommands">
        <button
          *ngIf="status === 'PENDING_INITIAL_REVIEW'"
          class="action-btn primary"
          (click)="navigateTo(['cases', caseId, 'initial-review'])">
          初審
        </button>
        <button
          *ngIf="status === 'PENDING_REREVIEW'"
          class="action-btn primary"
          (click)="navigateTo(['cases', caseId, 'rereview'])">
          複審
        </button>
        <button
          *ngIf="canAssign"
          class="action-btn"
          (click)="navigateTo(['cases', caseId, 'assign'])">
          指派
        </button>
      </ng-container>
    </div>
  `,
  styles: [`
    .row-actions { display: flex; gap: 6px; }
    .action-btn { padding: 2px 10px; border: 1px solid #1565c0; border-radius: 4px; cursor: pointer; background: #fff; color: #1565c0; font-size: 13px; }
    .action-btn.primary { background: #1565c0; color: #fff; }
  `]
})
export class CaseRowActionSetComponent {
  @Input() caseId = '';
  @Input() status = '';
  /** metadata-only 時 false → business commands hidden */
  @Input() showBusinessCommands = true;

  get canAssign(): boolean {
    return ['SELECTED_PENDING_ASSIGNMENT', 'PENDING_INITIAL_REVIEW'].includes(this.status);
  }

  constructor(private router: Router) {}

  navigateTo(segments: string[]): void {
    this.router.navigate(['/', ...segments]);
  }
}
