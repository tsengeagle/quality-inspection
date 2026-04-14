import { Component, Input } from '@angular/core';

/** C02 — StatusOrBlockedBanner: 呈現 terminal/forbidden/blocked/reason 訊息。
 *  支援三種 tone：blocked（暗紅）、exception-amber（琥珀警示）、info（資訊藍）。
 *  usedByPages: UI-010/011/012/013/014 */
@Component({
  selector: 'app-status-or-blocked-banner',
  templateUrl: './status-or-blocked-banner.component.html',
  styleUrls: ['./status-or-blocked-banner.component.scss']
})
export class StatusOrBlockedBannerComponent {
  /** 視覺語意 tone */
  @Input() tone: 'blocked' | 'exception-amber' | 'info' = 'info';
  /** 顯示訊息文字 */
  @Input() message = '';
}
