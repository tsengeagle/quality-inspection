import { Component, Input } from '@angular/core';

/** C11 — LifecycleProgressRegion (UI-010)
 *  顯示案件目前所處生命週期階段。 */
@Component({
  selector: 'app-lifecycle-progress-region',
  template: `
    <section class="lifecycle-progress" aria-label="生命週期進度">
      <h3 class="section-title">生命週期進度</h3>
      <div class="progress-row">
        <span class="label">目前階段：</span>
        <span class="value">{{ currentStage || '尚未建立' }}</span>
      </div>
    </section>
  `,
  styles: [`
    .lifecycle-progress { padding: 16px; border-bottom: 1px solid #e0e0e0; }
    .section-title { font-size: 14px; font-weight: 600; margin: 0 0 8px; }
    .progress-row { display: flex; gap: 8px; font-size: 14px; }
    .label { color: #666; min-width: 64px; }
    .value { color: #333; }
  `]
})
export class LifecycleProgressRegionComponent {
  @Input() currentStage = '';
}
