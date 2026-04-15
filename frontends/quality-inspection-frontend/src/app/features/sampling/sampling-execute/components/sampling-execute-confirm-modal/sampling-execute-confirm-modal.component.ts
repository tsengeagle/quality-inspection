import { Component, Input, Output, EventEmitter } from '@angular/core';

/** SP-07 — SamplingExecuteConfirmModal: 確認執行前彈窗 */
@Component({
  selector: 'app-sampling-execute-confirm-modal',
  template: `
    <div *ngIf="visible" class="modal-backdrop" (click)="cancelled.emit()">
      <div class="modal-dialog" role="dialog" aria-modal="true" aria-label="確認執行抽選" (click)="$event.stopPropagation()">
        <h2 class="modal-title">確認執行抽選</h2>
        <p class="modal-body">確認要執行此批次的抽選流程嗎？</p>
        <p class="modal-sub">執行後系統將依抽選規則自動選取符合條件的病歷案件。</p>
        <div class="modal-actions">
          <button class="btn cancel-btn" (click)="cancelled.emit()">取消</button>
          <button class="btn confirm-btn" (click)="confirmed.emit()">確認執行</button>
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
export class SamplingExecuteConfirmModalComponent {
  @Input() visible = false;
  @Input() batchId = '';
  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}
