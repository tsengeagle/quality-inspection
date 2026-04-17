import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SamplingBatchSummaryItem } from '../../../services/sampling.service';

/** SP-02 — BatchRowActions: 批次列表每列操作 */
@Component({
  selector: 'app-batch-row-actions',
  template: `
    <div class="row-actions">
      <button
        type="button"
        class="action-btn deferred"
        disabled
        title="Batch detail 已正式 deferred，等待 UI spec 定案。">
        查看詳情（未開放）
      </button>

      <button
        *ngIf="showViewCases"
        type="button"
        class="action-btn"
        (click)="viewCases.emit(batch.batchId)">
        查看案件
      </button>

      <button
        *ngIf="showRetry"
        type="button"
        class="action-btn retry"
        (click)="retryBatch.emit(batch.batchId)">
        重跑
      </button>
    </div>
  `,
  styles: [`
    .row-actions { display: flex; gap: 8px; }
    .action-btn { padding: 4px 10px; border: 1px solid #1565c0; border-radius: 4px; cursor: pointer; background: #fff; color: #1565c0; font-size: 13px; }
    .action-btn.retry { border-color: #e65100; color: #e65100; }
    .action-btn.deferred { border-color: #9e9e9e; color: #9e9e9e; cursor: not-allowed; }
  `]
})
export class BatchRowActionsComponent {
  @Input() batch!: SamplingBatchSummaryItem;
  @Input() role = 'HOSPITAL_MANAGER';
  @Output() viewCases = new EventEmitter<string>();
  @Output() retryBatch = new EventEmitter<string>();

  get showRetry(): boolean {
    return this.role === 'HOSPITAL_MANAGER' && this.batch.status === 'FAILED' && this.batch.canRetry;
  }

  get showViewCases(): boolean {
    return this.role === 'HOSPITAL_MANAGER' || this.role === 'AUDITOR';
  }
}
