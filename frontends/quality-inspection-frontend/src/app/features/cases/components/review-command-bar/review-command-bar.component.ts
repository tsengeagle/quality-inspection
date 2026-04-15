import { Component, EventEmitter, Input, Output } from '@angular/core';

/** C16 — ReviewCommandBar: viewport-fixed 底部 command bar。
 *  樣式強制 position: fixed; bottom: 0; width: 100%
 *  使用此元件的頁面必須加足夠的 padding-bottom 避免內容被遮蓋。
 *  usedByPages: UI-011/012/013 */
@Component({
  selector: 'app-review-command-bar',
  templateUrl: './review-command-bar.component.html',
  styleUrls: ['./review-command-bar.component.scss']
})
export class ReviewCommandBarComponent {
  /** 主要操作按鈕文字 */
  @Input() primaryLabel = '';
  /** 次要操作按鈕文字（選填） */
  @Input() secondaryLabel?: string;
  /** 是否禁用所有按鈕 */
  @Input() disabled = false;
  /** 主要按鈕獨立禁用狀態 */
  @Input() primaryDisabled = false;
  /** 次要按鈕獨立禁用狀態 */
  @Input() secondaryDisabled = false;

  @Output() primaryClick = new EventEmitter<void>();
  @Output() secondaryClick = new EventEmitter<void>();

  onPrimaryClick(): void {
    if (!this.disabled && !this.primaryDisabled) {
      this.primaryClick.emit();
    }
  }

  onSecondaryClick(): void {
    if (!this.disabled && !this.secondaryDisabled) {
      this.secondaryClick.emit();
    }
  }
}
