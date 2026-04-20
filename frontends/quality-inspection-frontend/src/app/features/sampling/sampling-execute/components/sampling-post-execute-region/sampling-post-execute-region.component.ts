import { Component, Input } from '@angular/core';

/** SP-06a — SamplingPostExecuteRegion: 顯示執行結果（COMPLETED） */
@Component({
  selector: 'app-sampling-post-execute-region',
  template: `
    <section class="post-execute-region" [class.post-execute-region--failed]="result?.tone === 'failed'">
      <div class="post-execute-region__header">
        <span class="post-execute-region__badge">{{ result?.tone === 'failed' ? '失敗' : '完成' }}</span>
        <h3 class="section-title">{{ result?.title ?? '執行結果' }}</h3>
      </div>
      <div *ngIf="result" class="result-content">
        <p class="result-reason" *ngIf="result.batchId">正式批次：{{ result.batchId }}</p>
        <p class="result-details" *ngIf="result.description">{{ result.description }}</p>
        <p class="result-details" *ngIf="result.startedAt">開始時間：{{ result.startedAt }}</p>
        <p class="result-details" *ngIf="result.finishedAt">完成時間：{{ result.finishedAt }}</p>
      </div>
    </section>
  `,
  styles: [`
    .post-execute-region {
      display: grid;
      gap: 12px;
      padding: 18px;
      border: 1px solid rgba(29, 122, 77, 0.2);
      border-radius: 14px;
      background: linear-gradient(180deg, rgba(242, 249, 245, 0.92) 0%, rgba(255, 255, 255, 1) 100%);
    }
    .post-execute-region--failed {
      border-color: rgba(168, 61, 61, 0.22);
      background: linear-gradient(180deg, rgba(255, 244, 244, 0.92) 0%, rgba(255, 255, 255, 1) 100%);
    }
    .post-execute-region__header { display: flex; gap: 10px; align-items: center; }
    .post-execute-region__badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 56px;
      height: 28px;
      border-radius: 999px;
      background: rgba(29, 122, 77, 0.12);
      color: #1d7a4d;
      font-size: 12px;
      font-weight: 700;
    }
    .post-execute-region--failed .post-execute-region__badge {
      background: rgba(168, 61, 61, 0.12);
      color: #9b2d30;
    }
    .section-title { font-size: 20px; font-weight: 700; margin: 0; color: var(--qi-text-primary); }
    .result-reason { font-size: 14px; margin: 0 0 4px; font-weight: 700; color: var(--qi-text-primary); }
    .result-details { font-size: 14px; margin: 0; color: var(--qi-text-secondary); line-height: 1.6; }
  `]
})
export class SamplingPostExecuteRegionComponent {
  @Input() result:
    | {
        tone: 'success' | 'failed';
        title: string;
        description: string;
        batchId: string | null;
        startedAt?: string | null;
        finishedAt?: string | null;
      }
    | undefined;
}
