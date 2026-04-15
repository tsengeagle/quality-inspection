import { Component, Input, Output, EventEmitter } from '@angular/core';

/** C05 — CaseWorkQueueFilterBar (UI-009)
 *  Role-aware filter chips，emit filter change 到 container。 */
@Component({
  selector: 'app-case-work-queue-filter-bar',
  template: `
    <div class="filter-bar">
      <button
        *ngFor="let s of availableStatuses"
        class="filter-chip"
        [class.active]="currentStatus === s.value"
        (click)="onStatusClick(s.value)">
        {{ s.label }}
      </button>
    </div>
  `,
  styles: [`
    .filter-bar { display: flex; gap: 8px; padding: 8px 16px; }
    .filter-chip { padding: 4px 12px; border: 1px solid #ccc; border-radius: 16px; cursor: pointer; background: #fff; }
    .filter-chip.active { background: #1565c0; color: #fff; border-color: #1565c0; }
  `]
})
export class CaseWorkQueueFilterBarComponent {
  @Input() role = '';
  @Input() currentStatus = 'PENDING';
  @Output() filterChange = new EventEmitter<string>();

  get availableStatuses(): Array<{ value: string; label: string }> {
    // Role-aware: 所有 role 都顯示基本 status chips
    return [
      { value: 'PENDING', label: '待處理' },
      { value: 'IN_PROGRESS', label: '進行中' },
      { value: 'CLOSED', label: '已結案' },
      { value: 'CANCELLED', label: '已取消' }
    ];
  }

  onStatusClick(status: string): void {
    this.filterChange.emit(status);
  }
}
