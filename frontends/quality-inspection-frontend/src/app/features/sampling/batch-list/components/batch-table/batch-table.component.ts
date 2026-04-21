import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SamplingBatchSummaryItem } from '../../../services/sampling.service';
import { AccessControlService } from '@shared/access/access-control.service';

/** SP-02 — BatchTable: 批次列表表格 */
@Component({
  selector: 'app-batch-table',
  template: `
    <div class="batch-table-wrapper">
      <div *ngIf="batches.length === 0" class="empty-hint">
        <strong>目前沒有符合條件的抽選批次。</strong>
        <span>請調整查詢條件，或稍後重新整理批次列表。</span>
      </div>

      <table *ngIf="batches.length > 0" class="batch-table">
        <thead>
          <tr>
            <th>批次 / 專案</th>
            <th>執行與狀態</th>
            <th>抽選摘要</th>
            <th>時間與重跑來源</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let b of batches" [attr.data-testid]="'batch-row-' + b.batchId">
            <td>
              <div class="cell-stack" [attr.data-testid]="'batch-row-summary-' + b.batchId">
                <strong>{{ b.batchName }}</strong>
                <span>專案：{{ b.projectName || b.projectId }}</span>
                <span>專案代碼：{{ b.projectId }}</span>
                <span *ngIf="b.projectVersionNumber != null">專案版本：v{{ b.projectVersionNumber }}</span>
              </div>
            </td>
            <td>
              <div class="cell-stack">
                <span class="status-badge" [attr.data-tone]="getStatusTone(b.status)">{{ b.status }}</span>
                <span>執行模式：{{ b.executionMode || '未提供' }}</span>
              </div>
            </td>
            <td>
              <div class="cell-stack" *ngIf="!isMetadataOnly; else metadataOnlySummary" [attr.data-testid]="'batch-row-counts-' + b.batchId">
                <span>母體件數：{{ b.populationCount ?? '-' }}</span>
                <span>符合件數：{{ b.eligibleCount ?? '-' }}</span>
                <span>排除件數：{{ b.excludedCount ?? '-' }}</span>
                <span>抽中件數：{{ b.selectedCount ?? '-' }}</span>
              </div>
              <ng-template #metadataOnlySummary>
                <div class="cell-stack">
                  <span>統計摘要：metadata-only</span>
                  <span>僅保留批次狀態與時間資訊</span>
                </div>
              </ng-template>
            </td>
            <td>
              <div class="cell-stack" [attr.data-testid]="'batch-row-timing-' + b.batchId">
                <span>開始時間：{{ b.createdAt || '-' }}</span>
                <span>完成時間：{{ b.finishedAt || '-' }}</span>
                <span>重跑來源：{{ b.retryOfBatchId || '無' }}</span>
              </div>
            </td>
            <td>
              <app-batch-row-actions
                [batch]="b"
                (viewCases)="viewCases.emit($event)"
                (retryBatch)="retryBatch.emit($event)">
              </app-batch-row-actions>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .batch-table-wrapper { padding: 0; }
    .empty-hint {
      display: grid;
      gap: 8px;
      padding: 20px;
      border: 1px dashed var(--qi-border-subtle);
      border-radius: 12px;
      color: var(--qi-text-secondary);
      font-size: 14px;
      background: linear-gradient(180deg, rgba(248, 250, 252, 0.8) 0%, rgba(255, 255, 255, 1) 100%);
    }
    .batch-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 14px;
      border: 1px solid var(--qi-border-subtle);
      border-radius: 12px;
      overflow: hidden;
    }
    .batch-table th,
    .batch-table td {
      padding: 10px 12px;
      border-bottom: 1px solid var(--qi-border-subtle);
      text-align: left;
      background: var(--qi-surface-base);
      color: var(--qi-text-primary);
      vertical-align: top;
    }
    .batch-table th {
      font-weight: 600;
      background: var(--qi-surface-subtle);
      color: var(--qi-text-secondary);
    }
    .cell-stack {
      display: grid;
      gap: 6px;
      min-width: 0;
    }
    .cell-stack strong {
      color: var(--qi-text-primary);
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: fit-content;
      min-height: 28px;
      padding: 0 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 700;
      background: rgba(10, 102, 194, 0.08);
      color: #0b5cab;
    }
    .status-badge[data-tone='failed'] {
      background: rgba(168, 61, 61, 0.12);
      color: #9b2d30;
    }
    .status-badge[data-tone='completed'] {
      background: rgba(29, 122, 77, 0.12);
      color: #1d7a4d;
    }
  `]
})
export class BatchTableComponent {
  @Input() batches: SamplingBatchSummaryItem[] = [];
  @Input() role = 'HOSPITAL_MANAGER';
  @Output() viewCases = new EventEmitter<string>();
  @Output() retryBatch = new EventEmitter<string>();

  constructor(private readonly accessControlService: AccessControlService) {}

  get isMetadataOnly(): boolean {
    return !this.accessControlService.hasAnyPermission('VIEW_SAMPLING_BATCH_METRICS');
  }

  getStatusTone(status: string): string {
    switch (status) {
      case 'FAILED':
        return 'failed';
      case 'COMPLETED':
        return 'completed';
      default:
        return 'progress';
    }
  }
}
