import { Component, Input } from '@angular/core';

/** C01 — CaseSummaryHeader: 統一承載 case 識別、stage/status、角色視角摘要。
 *  包含 C04 ProjectionNoticeBanner inline slot（透過 showProjectionNotice 控制）。
 *  usedByPages: UI-009/010/011/012/013/014 */
@Component({
  selector: 'app-case-summary-header',
  templateUrl: './case-summary-header.component.html',
  styleUrls: ['./case-summary-header.component.scss']
})
export class CaseSummaryHeaderComponent {
  @Input() caseId = '';
  @Input() caseTitle = '';
  @Input() status = '';
  @Input() stageLabel = '';
  /** 傳入 true 時在 header 內顯示 C04 ProjectionNoticeBanner */
  @Input() showProjectionNotice = false;
}
