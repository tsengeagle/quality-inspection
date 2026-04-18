import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-batch-filter-bar',
  template: `
    <div class="batch-filter-card">
      <div class="batch-filter-fields">
        <label class="field-group">
          <span>專案代碼</span>
          <input
            type="text"
            [ngModel]="projectId"
            (ngModelChange)="projectId = $event"
            placeholder="例如：f8f933a7-..."
            data-testid="batch-filter-project-id">
        </label>

        <label class="field-group">
          <span>批次代碼</span>
          <input
            type="text"
            [ngModel]="batchId"
            (ngModelChange)="batchId = $event"
            placeholder="例如：BATCH-20260418-01"
            data-testid="batch-filter-batch-id">
        </label>

        <div class="field-group" *ngIf="!metadataOnly">
          <span>資料範圍</span>
          <p class="scope-hint">依目前登入者院區與授權範圍查詢</p>
        </div>
      </div>

      <div class="batch-filter-actions">
        <button type="button" class="filter-action primary" (click)="onApplyFilters()">查詢</button>
        <button type="button" class="filter-action secondary" (click)="onResetFilters()">重置</button>
      </div>
    </div>
  `,
  styles: [`
    .batch-filter-card {
      margin: 0 16px 16px;
      padding: 12px;
      border: 1px solid var(--qi-border-subtle);
      border-radius: 8px;
      background: var(--qi-surface-base);
    }
    .batch-filter-fields {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 12px;
    }
    .field-group {
      display: grid;
      gap: 6px;
      font-size: 13px;
      color: var(--qi-text-secondary);
    }
    .field-group input {
      height: 36px;
      border: 1px solid var(--qi-border-subtle);
      border-radius: 6px;
      padding: 0 10px;
      color: var(--qi-text-primary);
      background: var(--qi-surface-base);
    }
    .scope-hint {
      margin: 0;
      min-height: 36px;
      display: flex;
      align-items: center;
      padding: 0 10px;
      border-radius: 6px;
      background: var(--qi-surface-subtle);
      color: var(--qi-text-secondary);
    }
    .batch-filter-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 12px;
    }
    .filter-action {
      height: 34px;
      border-radius: 6px;
      padding: 0 12px;
      border: 1px solid var(--qi-border-subtle);
      cursor: pointer;
    }
    .filter-action.primary {
      background: var(--qi-accent);
      border-color: var(--qi-accent);
      color: var(--qi-surface-base);
    }
    .filter-action.secondary {
      background: var(--qi-surface-base);
      color: var(--qi-text-primary);
    }
  `]
})
export class BatchFilterBarComponent {
  @Input() metadataOnly = false;
  @Output() filterChange = new EventEmitter<{ projectId?: string; batchId?: string }>();

  projectId = '';
  batchId = '';

  onApplyFilters(): void {
    this.filterChange.emit({
      projectId: this.projectId.trim(),
      batchId: this.batchId.trim()
    });
  }

  onResetFilters(): void {
    this.projectId = '';
    this.batchId = '';
    this.filterChange.emit({ projectId: '', batchId: '' });
  }
}
