import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SamplingBatchSummaryItem } from '../../../services/sampling.service';

/** SP-02 — BatchRowActions: 批次列表每列操作 */
@Component({
  selector: 'app-batch-row-actions',
  template: `
    <div class="row-actions">
      <!-- PH-01: [查看詳情] no-op STUB — 可見，但無路由；待 UI-010 / 詳情功能接入 -->
      <button class="action-btn" (click)="viewDetail.emit(batch.batchId)">查看詳情</button>

      <!-- SP-03 trigger: Retry — 只在 canRetry + ADMIN/MANAGER 時顯示 -->
      <button
        *ngIf="batch.canRetry && (role === 'ADMIN' || role === 'MANAGER')"
        class="action-btn retry"
        (click)="onRetry()">
        重試
      </button>
    </div>
  `,
  styles: [`
    .row-actions { display: flex; gap: 8px; }
    .action-btn { padding: 4px 10px; border: 1px solid #1565c0; border-radius: 4px; cursor: pointer; background: #fff; color: #1565c0; font-size: 13px; }
    .action-btn.retry { border-color: #e65100; color: #e65100; }
  `]
})
export class BatchRowActionsComponent {
  @Input() batch!: SamplingBatchSummaryItem;
  @Input() role = 'ADMIN';
  @Output() viewDetail = new EventEmitter<string>();
  @Output() retryBatch = new EventEmitter<string>();

  onRetry(): void {
    this.retryBatch.emit(this.batch.batchId);
  }
}
