import { Component, Input } from '@angular/core';

/** S-07 用到的 SubmissionRef: 代表被引用的 submission 版本識別。 */
export interface SubmissionRef {
  submissionId: string;
  version: number;
  submittedAt?: string;
}

/** C17 — ReviewVersionSummaryRegion: 顯示 REVISION mode 下的 basedOnVersion 參照摘要。
 *  僅在 REVISION mode 下顯示（isRevisionMode = true）；非 REVISION mode 不得渲染。
 *  usedByPages: UI-013 */
@Component({
  selector: 'app-review-version-summary-region',
  templateUrl: './review-version-summary-region.component.html',
  styleUrls: ['./review-version-summary-region.component.scss']
})
export class ReviewVersionSummaryRegionComponent {
  /** REVISION mode 的基準 submission 版本參照 */
  @Input() basedOnVersion?: SubmissionRef;
  /** 是否為 REVISION mode；僅 true 時渲染內容 */
  @Input() isRevisionMode = false;
}
