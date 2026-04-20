import { Component, Input } from '@angular/core';

/** SP-05a — SamplingRuleSummary: 顯示 ruleSnapshot 文字描述 */
@Component({
  selector: 'app-sampling-rule-summary',
  template: `
    <section class="rule-summary-card">
      <div class="rule-summary-card__header">
        <div>
          <p class="rule-summary-card__eyebrow">執行依據摘要</p>
          <h3 class="card-title">抽選規則摘要</h3>
        </div>
        <p class="rule-summary-card__copy">本區只顯示本次抽選執行依據，不在此頁直接修改規則。</p>
      </div>

      <ul class="rule-list" *ngIf="ruleLines.length > 0; else plainRule">
        <li *ngFor="let line of ruleLines">{{ line }}</li>
      </ul>

      <ng-template #plainRule>
        <p class="rule-text">{{ ruleSnapshot || '目前沒有可顯示的規則摘要。' }}</p>
      </ng-template>
    </section>
  `,
  styles: [`
    .rule-summary-card {
      display: grid;
      gap: 14px;
      padding: 18px;
      border: 1px solid var(--qi-border-subtle);
      border-radius: 14px;
      background: linear-gradient(180deg, rgba(250, 251, 253, 0.94) 0%, rgba(255, 255, 255, 1) 100%);
    }
    .rule-summary-card__header {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: flex-start;
    }
    .rule-summary-card__eyebrow {
      margin: 0 0 6px;
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: var(--qi-text-secondary);
    }
    .card-title { font-size: 20px; font-weight: 700; margin: 0; color: var(--qi-text-primary); }
    .rule-summary-card__copy {
      margin: 0;
      max-width: 420px;
      color: var(--qi-text-secondary);
      font-size: 14px;
      line-height: 1.6;
    }
    .rule-list {
      margin: 0;
      padding-left: 18px;
      display: grid;
      gap: 8px;
      color: var(--qi-text-primary);
      font-size: 14px;
      line-height: 1.6;
    }
    .rule-text { font-size: 14px; white-space: pre-wrap; word-break: break-word; margin: 0; color: var(--qi-text-primary); line-height: 1.7; }
  `]
})
export class SamplingRuleSummaryComponent {
  @Input() ruleSnapshot: string | undefined = '';

  get ruleLines(): string[] {
    return (this.ruleSnapshot ?? '')
      .split(/\n|•|；/)
      .map(line => line.trim())
      .filter(line => line.length > 0);
  }
}
