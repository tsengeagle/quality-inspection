import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-batch-filter-bar',
  template: `
    <div class="batch-filter-bar">
      <span class="filter-chip">projectId</span>
      <span class="filter-chip">batchId</span>
      <span class="filter-chip" *ngIf="!metadataOnly">hospital scope</span>
      <button type="button" class="filter-action" (click)="filterChange.emit({})">查詢</button>
      <button type="button" class="filter-action secondary" (click)="filterChange.emit({ projectId: '', batchId: '' })">重置</button>
    </div>
  `,
  styles: [`
    .batch-filter-bar { display: flex; gap: 8px; padding: 0 16px 16px; flex-wrap: wrap; align-items: center; }
    .filter-chip { border: 1px dashed currentColor; border-radius: 999px; padding: 4px 10px; font-size: 12px; }
    .filter-action { border: 1px solid currentColor; background: transparent; border-radius: 4px; padding: 4px 10px; }
    .filter-action.secondary { opacity: 0.75; }
  `]
})
export class BatchFilterBarComponent {
  @Input() metadataOnly = false;
  @Output() filterChange = new EventEmitter<{ projectId?: string; batchId?: string }>();
}
