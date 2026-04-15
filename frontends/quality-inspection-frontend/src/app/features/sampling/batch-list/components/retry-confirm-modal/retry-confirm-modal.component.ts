import { Component, Input, Output, EventEmitter } from '@angular/core';

/** SP-03 — RetryConfirmModal (UI-007)
 *  Fixed copy D-013: 確認重試此批次抽選？ */
@Component({
  selector: 'app-retry-confirm-modal',
  template: `
    <div *ngIf="visible" class="modal-backdrop" (click)="cancel.emit()">
      <div class="modal-dialog" role="dialog" aria-modal="true" aria-label="確認重試批次" (click)="$event.stopPropagation()">
        <h2 class="modal-title">確認重試</h2>
        <!-- D-013: 固定文案 -->
        <p class="modal-body">確認重試此批次抽選？</p>
        <p class="modal-sub">重試後系統將重新執行本批次的抽選流程。</p>
        <div class="modal-actions">
          <button class="btn cancel-btn" (click)="cancel.emit()">取消</button>
          <button class="btn confirm-btn" (click)="confirm.emit(batchId)">確認重試</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
    .modal-dialog { background: #fff; border-radius: 8px; padding: 24px; width: 400px; max-width: 90vw; }
    .modal-title { font-size: 18px; font-weight: 600; margin: 0 0 12px; }
    .modal-body { font-size: 15px; margin: 0 0 4px; }
    .modal-sub { font-size: 13px; color: #666; margin: 0 0 20px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 12px; }
    .btn { padding: 8px 20px; border-radius: 4px; cursor: pointer; font-size: 14px; font-weight: 600; border: none; }
    .cancel-btn { background: #f5f5f5; color: #333; }
    .confirm-btn { background: #1565c0; color: #fff; }
  `]
})
export class RetryConfirmModalComponent {
  @Input() visible = false;
  /** 目前要重試的 batchId */
  @Input() batchId = '';
  @Output() confirm = new EventEmitter<string>();
  @Output() cancel = new EventEmitter<void>();
}
