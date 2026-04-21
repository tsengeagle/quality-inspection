import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SamplingBatchSummaryItem } from '../../../services/sampling.service';
import { AccessCheck } from '@shared/access/access-control.model';

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
        查看詳情（Deferred）
      </button>

      <button
        *appHasPermission="'VIEW_SAMPLING_BATCH_CASES'"
        type="button"
        class="action-btn"
        (click)="viewCases.emit(batch.batchId)">
        查看案件
      </button>

      <ng-container *appCanAccess="retryAccessCheck">
        <button
          *ngIf="canRetryBatch"
          type="button"
          class="action-btn retry"
          (click)="retryBatch.emit(batch.batchId)">
          重跑
        </button>
      </ng-container>
    </div>
  `,
  styles: [`
    .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .action-btn {
      min-height: 34px;
      padding: 0 12px;
      border: 1px solid var(--qi-accent);
      border-radius: 8px;
      cursor: pointer;
      background: var(--qi-surface-base);
      color: var(--qi-accent);
      font-size: 13px;
      font-weight: 600;
    }
    .action-btn.retry { border-color: var(--qi-danger-text); color: var(--qi-danger-text); }
    .action-btn.deferred { border-color: var(--qi-border-muted); color: var(--qi-text-secondary); cursor: not-allowed; background: var(--qi-surface-subtle); }
  `]
})
export class BatchRowActionsComponent {
  @Input() batch!: SamplingBatchSummaryItem;
  @Output() viewCases = new EventEmitter<string>();
  @Output() retryBatch = new EventEmitter<string>();

  get canRetryBatch(): boolean {
    return this.batch.status === 'FAILED' && this.batch.canRetry;
  }

  get retryAccessCheck(): AccessCheck {
    return {
      anyPermissions: ['RETRY_SAMPLING_BATCH']
    };
  }
}
