import { Component, Input, Output, EventEmitter } from '@angular/core';

/** C22 — RevisionNavActionZone (UI-014)
 *  HR-2 強制規則：
 *  - `outlined warning-tone` 按鈕，不可 viewport-fixed
 *  - Navigate-only：`router.navigate` 前往複審作業頁
 *  - 只在 hasExceptionEntry 條件下由 parent 顯示 */
@Component({
  selector: 'app-revision-nav-action-zone',
  template: `
    <div class="revision-nav-action-zone">
      <!-- HR-2: outlined warning-tone button，絕不設為 position:fixed -->
      <button
        type="button"
        class="nav-action-btn outlined warning"
        (click)="navigateToRereview.emit()">
        前往複審修正
      </button>
    </div>
  `,
  styles: [`
    /* HR-2: 明確禁止 position:fixed — 使用 normal flow */
    .revision-nav-action-zone {
      padding: 16px;
      border-top: 1px solid #e0e0e0;
    }
    .nav-action-btn.outlined.warning {
      padding: 8px 20px;
      border: 2px solid #e65100;
      border-radius: 4px;
      cursor: pointer;
      background: #fff;
      color: #e65100;
      font-weight: 600;
    }
    .nav-action-btn.outlined.warning:hover {
      background: #fff3e0;
    }
  `]
})
export class RevisionNavActionZoneComponent {
  @Input() caseId = '';
  /** HR-2: emit navigate，不在此 component 自己 navigate */
  @Output() navigateToRereview = new EventEmitter<void>();
}
