import { Component, Input, Output, EventEmitter } from '@angular/core';

import { CaseStatus } from '../../../models/case-status';

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
  @Input() currentStatus: string | undefined;
  @Output() filterChange = new EventEmitter<CaseStatus>();

  /**
   * UI-009 §4.2「tab 與狀態對應建議」的七個單一狀態。每一個都是後端的 CaseStatus：
   * 列表的 status 只接受單一的狀態值（RC-LST-02），沒有「待處理」「進行中」這種合併的值。
   * INITIAL_REVIEW_COMPLETED、RETURNED 不在規格的清單裡，從「所有案件」頁籤看。
   */
  get availableStatuses(): Array<{ value: CaseStatus; label: string }> {
    return [
      { value: 'SELECTED_PENDING_ASSIGNMENT', label: '待分派' },
      { value: 'PENDING_INITIAL_REVIEW', label: '待初審' },
      { value: 'INITIAL_REVIEW_IN_PROGRESS', label: '初審中' },
      { value: 'PENDING_REREVIEW', label: '待複審' },
      { value: 'REREVIEW_IN_PROGRESS', label: '複審中' },
      { value: 'CLOSED', label: '已結案' },
      { value: 'CANCELLED', label: '已取消' }
    ];
  }

  onStatusClick(status: CaseStatus): void {
    this.filterChange.emit(status);
  }
}
