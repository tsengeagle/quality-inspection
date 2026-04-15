import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SamplingBatchSummaryItem } from '../../../services/sampling.service';

/** SP-02 — BatchRowActions: 批次列表每列操作 */
@Component({
  selector: 'app-batch-row-actions',
  template: `
    <div class="row-actions">
      <button type="button" class="action-btn" (click)="viewDetailStub.emit(batch.batchId)">查看詳情</button>

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
  `]
})
export class BatchRowActionsComponent {
  @Input() batch!: SamplingBatchSummaryItem;
  @Input() role = 'HOSPITAL_MANAGER';
  @Output() viewDetailStub = new EventEmitter<string>();
  @Output() viewCases = new EventEmitter<string>();
  @Output() retryBatch = new EventEmitter<string>();

  get showRetry(): boolean {
    return this.role === 'HOSPITAL_MANAGER' && this.batch.status === 'FAILED' && this.batch.canRetry;
  }

  get showViewCases(): boolean {
    return this.role === 'HOSPITAL_MANAGER' || this.role === 'AUDITOR';
  }
}
