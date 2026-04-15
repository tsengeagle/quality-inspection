import { Component, Input } from '@angular/core';

export interface PreviewStats {
  populationCount: number;
  eligibleCount: number;
  excludedCount: number;
}

/** SP-05b — SamplingPreviewStats: 只實作 authority 已確認的前 3 張卡片。 */
@Component({
  selector: 'app-sampling-preview-stats',
  template: `
    <section class="preview-stats panel panel-white p-3">
      <h3 class="section-title">預覽統計</h3>
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-label">母體件數</div>
          <div class="stat-value">{{ stats.populationCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">符合件數</div>
          <div class="stat-value">{{ stats.eligibleCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">排除件數</div>
          <div class="stat-value">{{ stats.excludedCount }}</div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .preview-stats { margin-bottom: 16px; }
    .section-title { font-size: 15px; font-weight: 600; margin: 0 0 12px; }
    .stat-cards { display: flex; gap: 16px; flex-wrap: wrap; }
    .stat-card { flex: 1 1 120px; border: 1px solid currentColor; border-radius: 6px; padding: 12px 16px; text-align: center; }
    .stat-label { font-size: 12px; margin-bottom: 4px; }
    .stat-value { font-size: 22px; font-weight: 700; }
  `]
})
export class SamplingPreviewStatsComponent {
  @Input() stats!: PreviewStats;
}
