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
      <div class="blocked-banner__icon">!</div>
      <div class="blocked-banner__content">
        <strong class="blocked-banner__title">目前不可執行抽選</strong>
        <span class="message">{{ messageText }}</span>
      </div>
    </div>
  `,
  styles: [`
    .blocked-banner {
      display: flex; align-items: flex-start; gap: 12px;
      border: 1px solid rgba(168, 61, 61, 0.22); border-radius: 14px;
      padding: 18px; margin-bottom: 16px;
      background: linear-gradient(180deg, rgba(255, 245, 245, 0.94) 0%, rgba(255, 255, 255, 1) 100%);
    }
    .blocked-banner__icon {
      width: 30px;
      height: 30px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
      background: rgba(168, 61, 61, 0.12);
      color: #9b2d30;
      font-size: 16px;
      font-weight: 700;
      line-height: 1;
    }
    .blocked-banner__content { display: grid; gap: 6px; }
    .blocked-banner__title { font-size: 15px; color: #7b1e22; }
    .message { font-size: 14px; color: #7b1e22; font-weight: 600; line-height: 1.6; }
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
