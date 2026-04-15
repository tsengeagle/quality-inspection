import { Component, Input } from '@angular/core';

/** SP-05a — SamplingRuleSummary: 顯示 ruleSnapshot 文字描述 */
@Component({
  selector: 'app-sampling-rule-summary',
  template: `
    <section class="rule-summary-card">
      <h3 class="card-title">抽選規則</h3>
      <pre class="rule-text">{{ ruleSnapshot }}</pre>
    </section>
  `,
  styles: [`
    .rule-summary-card { background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
    .card-title { font-size: 15px; font-weight: 600; margin: 0 0 8px; }
    .rule-text { font-size: 13px; white-space: pre-wrap; word-break: break-all; margin: 0; color: #333; }
  `]
})
export class SamplingRuleSummaryComponent {
  @Input() ruleSnapshot: string | undefined = '';
}
