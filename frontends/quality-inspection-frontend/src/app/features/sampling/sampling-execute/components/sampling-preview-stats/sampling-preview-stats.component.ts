import { Component, Input } from '@angular/core';

export interface PreviewStats {
  totalEligible: number;
  totalSelected: number;
  selectionRate: number;
}

/** SP-05b — SamplingPreviewStats: 3 stat cards (totalEligible / totalSelected / selectionRate) */
@Component({
  selector: 'app-sampling-preview-stats',
  template: `
    <section class="preview-stats">
      <h3 class="section-title">預覽統計</h3>
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-label">符合條件總數</div>
          <div class="stat-value">{{ stats.totalEligible }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已選取數量</div>
          <div class="stat-value">{{ stats.totalSelected }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">抽取率</div>
          <div class="stat-value">{{ stats.selectionRate | number:'1.1-1' }}%</div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .preview-stats { background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
    .section-title { font-size: 15px; font-weight: 600; margin: 0 0 12px; }
    .stat-cards { display: flex; gap: 16px; flex-wrap: wrap; }
    .stat-card { flex: 1 1 120px; background: #f5f8ff; border: 1px solid #d0ddf5; border-radius: 6px; padding: 12px 16px; text-align: center; }
    .stat-label { font-size: 12px; color: #666; margin-bottom: 4px; }
    .stat-value { font-size: 22px; font-weight: 700; color: #1565c0; }
  `]
})
export class SamplingPreviewStatsComponent {
  @Input() stats!: PreviewStats;
}
