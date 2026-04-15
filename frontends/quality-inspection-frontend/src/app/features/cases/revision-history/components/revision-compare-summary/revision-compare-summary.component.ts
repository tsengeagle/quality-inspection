import { Component, Input } from '@angular/core';
import { RevisionHistoryMode } from '../../revision-history-page.component';

/** C21 — RevisionCompareSummary (UI-014)
 *  FULL: 顯示 diff skeleton；
 *  HR-1 (METADATA_HISTORY): 顯示固定訊息 "詳細差異內容不適用於此視角"。 */
@Component({
  selector: 'app-revision-compare-summary',
  template: `
    <section class="compare-summary" aria-label="Revision 差異摘要">
      <h3 class="section-title">差異摘要</h3>

      <!-- HR-1: METADATA_HISTORY 強制顯示此訊息，不顯示 diff -->
      <div *ngIf="showMetadataOnlyNotice" class="metadata-notice">
        詳細差異內容不適用於此視角。
      </div>

      <!-- FULL_HISTORY / READONLY_TERMINAL: 顯示 diff skeleton -->
      <div *ngIf="!showMetadataOnlyNotice" class="diff-skeleton">
        <div class="strategy-note">版本選擇互動尚未定案；目前採固定差異摘要檢視。</div>
        <div class="diff-placeholder">（Diff 比較區塊 — 待 API 接入後替換）</div>
      </div>
    </section>
  `,
  styles: [`
    .compare-summary { padding: 16px; border-top: 1px solid #e0e0e0; }
    .section-title { font-size: 14px; font-weight: 600; margin: 0 0 8px; }
    .metadata-notice { color: #5c5c5c; font-style: italic; font-size: 14px; }
    .strategy-note { color: #666; font-size: 13px; margin-bottom: 8px; }
    .diff-placeholder { color: #bbb; font-size: 13px; padding: 8px; border: 1px dashed #e0e0e0; border-radius: 4px; }
  `]
})
export class RevisionCompareSummaryComponent {
  @Input() mode: RevisionHistoryMode = 'READONLY_TERMINAL';
  /** HR-1: true → 顯示不適用訊息 */
  @Input() showMetadataOnlyNotice = false;
}
