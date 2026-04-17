import { Component, Input } from '@angular/core';
import { BlockedReason } from '../../sampling-execute-page.component';

/** SP-06b — SamplingBlockedBanner: minimal READONLY_BLOCKED banner
 *  tone: blocked（符合 HR 規則，outlined warning tone） */
const BLOCKED_MESSAGES: Record<string, string> = {
  PROJECT_NOT_ACTIVE: '專案尚未啟用，無法執行抽選。',
  PROJECT_VERSION_NOT_PUBLISHED: '尚無正式版本，不可執行抽選。',
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
