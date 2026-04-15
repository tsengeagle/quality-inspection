import { Component, Input } from '@angular/core';

/** C10 — RereviewSummaryRegion (UI-010)
 *  顯示最新 revision 摘要；詳細差異仍由 UI-014 compare 區承接。 */
@Component({
  selector: 'app-rereview-summary-region',
  template: `
    <section class="rereview-summary" aria-label="複審摘要">
      <h3 class="section-title">複審摘要</h3>
      <div *ngIf="summary; else noSummary" class="summary-content">{{ summary }}</div>
      <ng-template #noSummary>
        <span class="empty-hint">尚無複審摘要。</span>
      </ng-template>
    </section>
  `,
  styles: [`
    .rereview-summary { padding: 16px; border-bottom: 1px solid #e0e0e0; }
    .section-title { font-size: 14px; font-weight: 600; margin: 0 0 8px; }
    .summary-content { font-size: 14px; color: #333; }
    .empty-hint { font-size: 13px; color: #999; }
  `]
})
export class RereviewSummaryRegionComponent {
  @Input() summary?: string;
}
