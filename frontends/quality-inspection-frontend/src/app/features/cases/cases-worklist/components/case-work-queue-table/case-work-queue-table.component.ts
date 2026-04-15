import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { CaseSummaryItem } from '../../../services/cases.service';

/** C06 — CaseWorkQueueTable (UI-009)
 *  顯示案件列表；metadata-only 時只顯示 shared base whitelist 欄位，隱藏 business commands。 */
@Component({
  selector: 'app-case-work-queue-table',
  template: `
    <div class="table-container">
      <!-- 空狀態 -->
      <div *ngIf="cases.length === 0" class="empty-state">
        目前沒有案件。
      </div>

      <!-- 資料列 -->
      <table *ngIf="cases.length > 0" class="case-table">
        <thead>
          <tr>
            <th>案件編號</th>
            <th>病歷號</th>
            <th>狀態</th>
            <th>階段</th>
            <th>建立日期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let c of cases">
            <td>{{ c.caseId }}</td>
            <td>{{ c.patientId }}</td>
            <td>{{ c.status }}</td>
            <td>{{ c.stageLabel }}</td>
            <td>{{ c.createDate }}</td>
            <td>
              <app-case-row-action-set
                [caseId]="c.caseId"
                [status]="c.status"
                [showBusinessCommands]="showBusinessCommands">
              </app-case-row-action-set>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .table-container { padding: 16px; }
    .empty-state { color: #666; padding: 32px; text-align: center; }
    .case-table { width: 100%; border-collapse: collapse; }
    .case-table th, .case-table td { border-bottom: 1px solid #e0e0e0; padding: 8px 12px; text-align: left; }
    .case-table th { background: #f5f5f5; font-weight: 600; }
  `]
})
export class CaseWorkQueueTableComponent {
  @Input() cases: CaseSummaryItem[] = [];
  /** metadata-only 時設為 false → business commands hidden */
  @Input() showBusinessCommands = true;
}
