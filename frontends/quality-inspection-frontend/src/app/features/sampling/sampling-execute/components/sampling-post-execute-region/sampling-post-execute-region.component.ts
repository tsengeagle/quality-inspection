import { Component, Input } from '@angular/core';

/** SP-06a — SamplingPostExecuteRegion: 顯示執行結果（COMPLETED） */
@Component({
  selector: 'app-sampling-post-execute-region',
  template: `
    <section class="post-execute-region panel panel-white p-3">
      <h3 class="section-title">{{ result?.title ?? '執行結果' }}</h3>
      <div *ngIf="result" class="result-content">
        <p class="result-reason" *ngIf="result.batchId">batchId: {{ result.batchId }}</p>
        <p class="result-details" *ngIf="result.description">{{ result.description }}</p>
      </div>
    </section>
  `,
  styles: [`
    .post-execute-region { margin-bottom: 16px; }
    .section-title { font-size: 15px; font-weight: 600; margin: 0 0 8px; }
    .result-reason { font-size: 14px; margin: 0 0 4px; font-weight: 600; }
    .result-details { font-size: 13px; margin: 0; }
  `]
})
export class SamplingPostExecuteRegionComponent {
  @Input() result: { tone: 'success' | 'failed'; title: string; description: string; batchId: string | null } | undefined;
}
