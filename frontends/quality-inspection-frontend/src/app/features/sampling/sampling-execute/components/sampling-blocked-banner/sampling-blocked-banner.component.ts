import { Component, Input } from '@angular/core';
import { BlockedReason } from '../../sampling-execute-page.component';

/** SP-06b — SamplingBlockedBanner: 4-reason READONLY_BLOCKED banner
 *  tone: blocked（符合 HR 規則，outlined warning tone） */
const BLOCKED_MESSAGES: Record<string, string> = {
  ALREADY_EXECUTED:  '此批次已執行完成，不允許重複執行。',
  NO_PERMISSION:     '您目前沒有執行此抽選批次的權限。',
  PROJECT_CLOSED:    '此審查專案已關閉，無法執行抽選。',
  OTHER:             '目前無法執行此批次，請聯絡系統管理員。',
  NONE:              ''
};

@Component({
  selector: 'app-sampling-blocked-banner',
  template: `
    <div class="blocked-banner" *ngIf="reason !== 'NONE'">
      <span class="icon">⚠</span>
      <span class="message">{{ messageText }}</span>
    </div>
  `,
  styles: [`
    .blocked-banner {
      display: flex; align-items: flex-start; gap: 10px;
      background: #fff3e0; border: 1px solid #e65100; border-radius: 6px;
      padding: 14px 16px; margin-bottom: 16px;
    }
    .icon { font-size: 18px; color: #e65100; line-height: 1.4; }
    .message { font-size: 14px; color: #e65100; font-weight: 600; line-height: 1.6; }
  `]
})
export class SamplingBlockedBannerComponent {
  @Input() reason: BlockedReason = 'NONE';

  get messageText(): string {
    return BLOCKED_MESSAGES[this.reason] ?? BLOCKED_MESSAGES['OTHER'];
  }
}
