import { Component, Input } from '@angular/core';

/** C15 — ReviewFormSummaryStrip: 審查表單摘要列（只顯示摘要資訊，不含表單欄位或操作按鈕）。
 *  usedByPages: UI-012/013 */
@Component({
  selector: 'app-review-form-summary-strip',
  templateUrl: './review-form-summary-strip.component.html',
  styleUrls: ['./review-form-summary-strip.component.scss']
})
export class ReviewFormSummaryStripComponent {
  /** 當前作業 mode（INITIAL_REVIEW / REREVIEW / REVISION…） */
  @Input() mode = '';
}
