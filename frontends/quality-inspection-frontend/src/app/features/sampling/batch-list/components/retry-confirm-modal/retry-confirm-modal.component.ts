import { Component, Input, Output, EventEmitter } from '@angular/core';

/** SP-03 — RetryConfirmModal (UI-007)
 *  Fixed copy D-013: 將以新的抽選批次重跑，原失敗批次會保留。 */
@Component({
  selector: 'app-retry-confirm-modal',
  template: `
    <div *ngIf="visible" class="modal-backdrop" (click)="cancel.emit()">
      <div class="modal-dialog" role="dialog" aria-modal="true" aria-label="確認重試批次" (click)="$event.stopPropagation()">
        <h2 class="modal-title">確認重跑</h2>
        <p class="modal-body">將以新的抽選批次重跑，原失敗批次會保留。</p>
        <p class="modal-sub">此操作不會覆蓋原批次，也不得暗示改用最新版本重跑。</p>
        <div class="modal-actions">
          <button class="btn cancel-btn" (click)="cancel.emit()">取消</button>
          <button class="btn confirm-btn" (click)="confirm.emit(batchId)">確認重跑</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0, 0, 0, .35); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-dialog { background: var(--qi-surface-base); border-radius: 8px; border: 1px solid var(--qi-border-subtle); padding: 24px; width: 400px; max-width: 90vw; }
    .modal-title { font-size: 18px; font-weight: 600; margin: 0 0 12px; }
    .modal-body { font-size: 15px; margin: 0 0 4px; }
    .modal-sub { font-size: 13px; color: var(--qi-text-secondary); margin: 0 0 20px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
    .btn { padding: 8px 20px; border-radius: 4px; cursor: pointer; font-size: 14px; font-weight: 600; border: none; }
    .cancel-btn { background: var(--qi-surface-subtle); color: var(--qi-text-primary); }
    .confirm-btn { background: var(--qi-accent); color: var(--qi-surface-base); }
  `]
})
export class RetryConfirmModalComponent {
  @Input() visible = false;
  /** 目前要重試的 batchId */
  @Input() batchId = '';
  @Output() confirm = new EventEmitter<string>();
  @Output() cancel = new EventEmitter<void>();
}
