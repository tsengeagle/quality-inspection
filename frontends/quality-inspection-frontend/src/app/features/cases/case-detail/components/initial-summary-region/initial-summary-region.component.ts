import { Component, Input } from '@angular/core';

/** C09 — InitialSummaryRegion (UI-010)
 *  展示初審摘要資訊（若有）。 */
@Component({
  selector: 'app-initial-summary-region',
  template: `
    <section class="initial-summary" aria-label="初審摘要">
      <h3 class="section-title">初審摘要</h3>
      <div *ngIf="summary; else noSummary" class="summary-content">{{ summary }}</div>
      <ng-template #noSummary>
        <span class="empty-hint">尚無初審摘要。</span>
      </ng-template>
    </section>
  `,
  styles: [`
    .initial-summary { padding: 16px; border-bottom: 1px solid #e0e0e0; }
    .section-title { font-size: 14px; font-weight: 600; margin: 0 0 8px; }
    .summary-content { font-size: 14px; color: #333; }
    .empty-hint { font-size: 13px; color: #999; }
  `]
})
export class InitialSummaryRegionComponent {
  @Input() caseId = '';
  /** 初審摘要文字（stub，待 API 接入後替換） */
  @Input() summary?: string;
}
