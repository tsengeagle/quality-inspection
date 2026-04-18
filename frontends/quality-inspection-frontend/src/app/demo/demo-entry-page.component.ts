import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { demoEntryConfig } from './demo-entry.config';

@Component({
  selector: 'app-demo-entry-page',
  standalone: true,
  template: `
    <main class="demo-entry-page" data-testid="demo-entry-page">
      <header class="demo-entry-header">
        <h1>品質審查系統 Demo 入口</h1>
        <p>請從以下路徑進入展示流程，避免手動輸入長網址造成流程中斷。</p>
      </header>

      <section class="demo-entry-grid">
        <article class="entry-card">
          <h2>UI-007 抽選批次列表</h2>
          <p>查看批次狀態、切換條件並導向案件列表。</p>
          <button type="button" class="entry-link" (click)="goToBatchList()">進入 UI-007</button>
        </article>

        <article class="entry-card">
          <h2>UI-006 抽選執行</h2>
          <p>預估抽選、正式執行並可前往批次列表。</p>
          <button type="button" class="entry-link" (click)="goToSamplingExecute()">進入 UI-006</button>
        </article>

        <article class="entry-card">
          <h2>UI-011 案件分派</h2>
          <p>提交初審或複審指派，完成後可返回案件詳情。</p>
          <button type="button" class="entry-link" (click)="goToCaseAssignment()">進入 UI-011</button>
        </article>
      </section>

      <section class="demo-entry-footnote">
        <p>預設展示資料：</p>
        <ul>
          <li>projectId: {{ config.defaultProjectId }}</li>
          <li>caseId: {{ config.defaultCaseId }}</li>
        </ul>
      </section>
    </main>
  `,
  styles: [`
    .demo-entry-page {
      max-width: 1080px;
      margin: 0 auto;
      padding: 24px;
      display: grid;
      gap: 16px;
    }
    .demo-entry-header {
      border: 1px solid var(--qi-border-subtle);
      border-radius: 12px;
      padding: 18px;
      background: var(--qi-surface-base);
    }
    .demo-entry-header h1 {
      margin: 0;
      font-size: 26px;
    }
    .demo-entry-header p {
      margin: 8px 0 0;
      color: var(--qi-text-secondary);
    }
    .demo-entry-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 14px;
    }
    .entry-card {
      border: 1px solid var(--qi-border-subtle);
      border-radius: 10px;
      padding: 14px;
      background: var(--qi-surface-base);
      display: grid;
      gap: 8px;
    }
    .entry-card h2 {
      margin: 0;
      font-size: 18px;
    }
    .entry-card p {
      margin: 0;
      color: var(--qi-text-secondary);
      min-height: 40px;
    }
    .entry-link {
      display: inline-flex;
      width: fit-content;
      align-items: center;
      justify-content: center;
      height: 34px;
      padding: 0 12px;
      border-radius: 6px;
      border: none;
      color: var(--qi-surface-base);
      background: var(--qi-accent);
      font-weight: 600;
      cursor: pointer;
    }
    .demo-entry-footnote {
      border-left: 3px solid var(--qi-accent);
      padding: 8px 12px;
      color: var(--qi-text-secondary);
      background: var(--qi-surface-base);
    }
    .demo-entry-footnote p {
      margin: 0 0 6px;
    }
    .demo-entry-footnote ul {
      margin: 0;
      padding-left: 20px;
    }
  `]
})
export class DemoEntryPageComponent {
  readonly config = demoEntryConfig;

  constructor(private readonly router: Router) {}

  goToBatchList(): void {
    this.router.navigate(['/sampling/batches']);
  }

  goToSamplingExecute(): void {
    this.router.navigate(['/sampling', this.config.defaultProjectId]);
  }

  goToCaseAssignment(): void {
    this.router.navigate(['/cases', this.config.defaultCaseId, 'assign']);
  }
}
