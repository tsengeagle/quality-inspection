import { Component, Input, Output, EventEmitter } from '@angular/core';
import { SamplingBatchSummaryItem } from '../../../services/sampling.service';

/** SP-02 — BatchTable: 批次列表表格 */
@Component({
  selector: 'app-batch-table',
  template: `
    <div class="batch-table-wrapper">
      <div *ngIf="batches.length === 0" class="empty-hint">目前沒有抽選批次。</div>

      <table *ngIf="batches.length > 0" class="batch-table">
        <thead>
          <tr>
            <th>批次名稱</th>
            <th>狀態</th>
            <th>建立日期</th>
            <th>專案</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let b of batches">
            <td>{{ b.batchName }}</td>
            <td>{{ b.status }}</td>
            <td>{{ b.createdAt }}</td>
            <td>{{ b.projectName }}</td>
            <td>
              <app-batch-row-actions
                [batch]="b"
                [role]="role"
                (viewDetailStub)="viewDetailStub.emit($event)"
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
    .batch-table-wrapper { padding: 0 16px 16px; }
    .empty-hint { color: #999; font-size: 13px; padding: 16px 0; }
    .batch-table { width: 100%; border-collapse: collapse; font-size: 14px; }
    .batch-table th, .batch-table td { padding: 10px 12px; border-bottom: 1px solid #e0e0e0; text-align: left; }
    .batch-table th { font-weight: 600; background: #f5f5f5; }
  `]
})
export class BatchTableComponent {
  @Input() batches: SamplingBatchSummaryItem[] = [];
  @Input() role = 'HOSPITAL_MANAGER';
  @Output() viewDetailStub = new EventEmitter<string>();
  @Output() viewCases = new EventEmitter<string>();
  @Output() retryBatch = new EventEmitter<string>();
}
