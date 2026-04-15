import { Component, Input, Output, EventEmitter } from '@angular/core';

/** SP-02 — BatchStatusTab: 批次狀態 filter chips */
@Component({
  selector: 'app-batch-status-tab',
  template: `
    <div class="batch-status-tab">
      <button
        *ngFor="let tab of tabs"
        class="chip"
        type="button"
        [class.active]="activeTab === tab.value"
        (click)="tabChange.emit(tab.value)">
        {{ tab.label }}
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
  @Input() tabs: Array<{ value: string; label: string }> = [];
  @Input() activeTab = '';
  @Output() tabChange = new EventEmitter<string>();
}
