import { Component, Input } from '@angular/core';
import { BlockedReason } from '../../sampling-execute-page.component';

/** SP-06b — SamplingBlockedBanner: 4-reason READONLY_BLOCKED banner
 *  tone: blocked（符合 HR 規則，outlined warning tone） */
const BLOCKED_MESSAGES: Record<string, string> = {
  PROJECT_NOT_ACTIVE: '專案尚未啟用，無法執行抽選。',
  COMPLETED_BATCH_EXISTS: '同版本已有完成批次，不能再次執行。',
  PROJECT_CLOSED: '此審查專案已關閉，無法執行抽選。',
  PERMISSION_REVOKED: '您目前沒有執行此抽選頁面的權限。',
  OTHER:             '目前無法執行此批次，請聯絡系統管理員。',
  NONE:              ''
};

@Component({
  selector: 'app-sampling-blocked-banner',
  template: `
    <div class="blocked-banner" *ngIf="reason">
      <span class="icon">!</span>
      <span class="message">{{ messageText }}</span>
    </div>
  `,
  styles: [`
    .blocked-banner {
      display: flex; align-items: flex-start; gap: 10px;
      border: 1px solid currentColor; border-radius: 6px;
      padding: 14px 16px; margin-bottom: 16px;
    }
    .icon { font-size: 18px; line-height: 1.4; }
    .message { font-size: 14px; font-weight: 600; line-height: 1.6; }
  `]
})
export class SamplingBlockedBannerComponent {
  @Input() reason: BlockedReason = null;

  get messageText(): string {
    if (!this.reason) {
      return '';
    }

    return BLOCKED_MESSAGES[this.reason] ?? BLOCKED_MESSAGES['OTHER'];
  }
}
