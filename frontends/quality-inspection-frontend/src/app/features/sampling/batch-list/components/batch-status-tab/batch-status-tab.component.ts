import { Component, Input, Output, EventEmitter } from '@angular/core';

/** 批次狀態 chips */
const BATCH_STATUSES = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待執行' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'FAILED', label: '失敗' },
  { value: 'CANCELLED', label: '已取消' }
];

/** SP-02 — BatchStatusTab: 批次狀態 filter chips */
@Component({
  selector: 'app-batch-status-tab',
  template: `
    <div class="batch-status-tab">
      <button
        *ngFor="let s of statuses"
        class="chip"
        [class.active]="currentStatus === s.value"
        (click)="statusChange.emit(s.value)">
        {{ s.label }}
      </button>
    </div>
  `,
  styles: [`
    .batch-status-tab { display: flex; gap: 8px; padding: 8px 16px; flex-wrap: wrap; }
    .chip { padding: 4px 12px; border: 1px solid #ccc; border-radius: 16px; cursor: pointer; background: #fff; font-size: 13px; }
    .chip.active { background: #1565c0; color: #fff; border-color: #1565c0; }
  `]
})
export class BatchStatusTabComponent {
  @Input() currentStatus = '';
  @Output() statusChange = new EventEmitter<string>();

  readonly statuses = BATCH_STATUSES;
}
