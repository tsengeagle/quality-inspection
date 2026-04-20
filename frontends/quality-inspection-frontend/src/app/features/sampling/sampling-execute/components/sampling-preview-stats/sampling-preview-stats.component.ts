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
    <section class="preview-stats">
      <div class="preview-stats__header">
        <div>
          <p class="section-eyebrow">預估結果</p>
          <h3 class="section-title">預估統計摘要</h3>
        </div>
        <p class="preview-stats__copy">以母體、符合條件、排除件數三個核心指標，支撐正式執行前的確認動作。</p>
      </div>
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-label">母體件數</div>
          <div class="stat-value">{{ stats.populationCount | number }}</div>
          <div class="stat-caption">抽選來源總件數</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">符合件數</div>
          <div class="stat-value">{{ stats.eligibleCount | number }}</div>
          <div class="stat-caption">符合目前規則的案件</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">排除件數</div>
          <div class="stat-value">{{ stats.excludedCount | number }}</div>
          <div class="stat-caption">因排除條件未納入抽樣</div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .preview-stats {
      display: grid;
      gap: 16px;
      padding: 18px;
      border: 1px solid var(--qi-border-subtle);
      border-radius: 14px;
      background: var(--qi-surface-base);
    }
    .preview-stats__header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }
    .section-eyebrow {
      margin: 0 0 6px;
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--qi-text-secondary);
    }
    .section-title { font-size: 20px; font-weight: 700; margin: 0; color: var(--qi-text-primary); }
    .preview-stats__copy { margin: 0; max-width: 420px; color: var(--qi-text-secondary); font-size: 14px; line-height: 1.6; }
    .stat-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
    .stat-card {
      display: grid;
      gap: 8px;
      border: 1px solid var(--qi-border-subtle);
      border-radius: 12px;
      padding: 16px;
      background: linear-gradient(180deg, rgba(248, 250, 252, 0.8) 0%, rgba(255, 255, 255, 1) 100%);
    }
    .stat-label { font-size: 12px; margin-bottom: 2px; color: var(--qi-text-secondary); font-weight: 700; }
    .stat-value { font-size: 30px; font-weight: 700; color: var(--qi-text-primary); }
    .stat-caption { font-size: 13px; color: var(--qi-text-secondary); line-height: 1.5; }
  `]
})
export class SamplingPreviewStatsComponent {
  @Input() stats!: PreviewStats;
}
