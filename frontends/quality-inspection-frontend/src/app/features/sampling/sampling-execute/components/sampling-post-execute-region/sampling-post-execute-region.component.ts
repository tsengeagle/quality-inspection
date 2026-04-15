import { Component, Input } from '@angular/core';

/** SP-06a — SamplingPostExecuteRegion: 顯示執行結果（COMPLETED） */
@Component({
  selector: 'app-sampling-post-execute-region',
  template: `
    <section class="post-execute-region">
      <h3 class="section-title">執行結果</h3>
      <div *ngIf="result" class="result-content">
        <p class="result-reason" *ngIf="result.reason">{{ result.reason }}</p>
        <p class="result-details" *ngIf="result.details">{{ result.details }}</p>
      </div>
    </section>
  `,
  styles: [`
    .post-execute-region { background: #e8f5e9; border: 1px solid #a5d6a7; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
    .section-title { font-size: 15px; font-weight: 600; margin: 0 0 8px; color: #2e7d32; }
    .result-reason { font-size: 14px; margin: 0 0 4px; font-weight: 600; }
    .result-details { font-size: 13px; color: #444; margin: 0; }
  `]
})
export class SamplingPostExecuteRegionComponent {
  @Input() result: { reason: string; details: string } | undefined;
}
