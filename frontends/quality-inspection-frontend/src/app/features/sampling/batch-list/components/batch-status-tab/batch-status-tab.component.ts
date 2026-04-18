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
    .batch-status-tab { display: flex; gap: 8px; padding: 4px 0; flex-wrap: wrap; }
    .chip {
      height: 32px;
      padding: 0 12px;
      border: 1px solid var(--qi-border-subtle);
      border-radius: 16px;
      cursor: pointer;
      background: var(--qi-surface-base);
      color: var(--qi-text-secondary);
      font-size: 13px;
    }
    .chip.active { background: var(--qi-accent); color: var(--qi-surface-base); border-color: var(--qi-accent); }
  `]
})
export class BatchStatusTabComponent {
  @Input() tabs: Array<{ value: string; label: string }> = [];
  @Input() activeTab = '';
  @Output() tabChange = new EventEmitter<string>();
}
