import { Component, Input } from '@angular/core';

/** C10/C11 — ReviewerAssignmentInfo (UI-010)
 *  展示審查員與複審員指派資訊。 */
@Component({
  selector: 'app-reviewer-assignment-info',
  template: `
    <section class="assignment-info" aria-label="指派資訊">
      <h3 class="section-title">指派資訊</h3>
      <div class="info-row">
        <span class="label">初審員：</span>
        <span class="value">{{ reviewerName || '（未指派）' }}</span>
      </div>
      <div class="info-row">
        <span class="label">複審員：</span>
        <span class="value">{{ rereviewerName || '（未指派）' }}</span>
      </div>
    </section>
  `,
  styles: [`
    .assignment-info { padding: 16px; border-bottom: 1px solid #e0e0e0; }
    .section-title { font-size: 14px; font-weight: 600; margin: 0 0 8px; }
    .info-row { display: flex; gap: 8px; margin-bottom: 4px; font-size: 14px; }
    .label { color: #666; min-width: 64px; }
    .value { color: #333; }
  `]
})
export class ReviewerAssignmentInfoComponent {
  @Input() reviewerName?: string;
  @Input() rereviewerName?: string;
}
