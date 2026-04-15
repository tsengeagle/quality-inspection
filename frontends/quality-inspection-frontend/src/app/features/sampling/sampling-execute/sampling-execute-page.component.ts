import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SamplingService, SamplingBatchDetailView } from '../services/sampling.service';

/** SP-04 — SamplingExecutePage 6-mode (UI-006)
 *  IDLE: 待確認 — 尚未執行
 *  PREVIEW: 預覽中 — 已取得預覽統計，等待確認執行
 *  EXECUTING: 執行中
 *  COMPLETED: 執行成功
 *  BLOCKED_ALREADY_EXECUTED: 已執行（不可重複）
 *  BLOCKED_NO_PERMISSION: 無執行權限 */
export type SamplingExecuteMode =
  | 'IDLE'
  | 'PREVIEW'
  | 'EXECUTING'
  | 'COMPLETED'
  | 'BLOCKED_ALREADY_EXECUTED'
  | 'BLOCKED_NO_PERMISSION';

/** 4 種 BLOCKED reason（SP-06 banner）*/
export type BlockedReason =
  | 'ALREADY_EXECUTED'
  | 'NO_PERMISSION'
  | 'NONE';

@Component({
  selector: 'app-sampling-execute-page',
  templateUrl: './sampling-execute-page.component.html',
  styleUrls: ['./sampling-execute-page.component.scss']
})
export class SamplingExecutePageComponent implements OnInit {

  batchId = '';
  vm: SamplingBatchDetailView | null = null;

  mode: SamplingExecuteMode = 'IDLE';
  blockedReason: BlockedReason = 'NONE';
  modeLabel = '';

  /** SP-07: 確認執行 modal 開關 */
  showConfirmModal = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private samplingService: SamplingService
  ) {}

  ngOnInit(): void {
    this.batchId = this.route.snapshot.paramMap.get('batchId') ?? '';
    this.samplingService.getBatchDetail(this.batchId).subscribe(vm => {
      this.vm = vm;
      this._resolveMode();
    });
  }

  /** 6-mode resolution */
  private _resolveMode(): void {
    const d = this.vm;
    if (!d) {
      this.mode = 'IDLE';
      this._applyMode();
      return;
    }

    if (d.status === 'COMPLETED') {
      this.mode = 'COMPLETED';
      this.blockedReason = 'NONE';
    } else if (d.status === 'PENDING') {
      this.mode = 'IDLE';
      this.blockedReason = 'NONE';
    } else if (d.status === 'EXECUTING') {
      this.mode = 'EXECUTING';
      this.blockedReason = 'NONE';
    } else if (d.status === 'BLOCKED') {
      this.mode = 'BLOCKED_ALREADY_EXECUTED';
      this.blockedReason = 'ALREADY_EXECUTED';
    } else {
      // 預覽後尚未送出: PREVIEW
      this.mode = d.previewStats ? 'PREVIEW' : 'IDLE';
      this.blockedReason = 'NONE';
    }
    this._applyMode();
  }

  private _applyMode(): void {
    switch (this.mode) {
      case 'IDLE':                     this.modeLabel = '待執行'; break;
      case 'PREVIEW':                  this.modeLabel = '預覽中'; break;
      case 'EXECUTING':                this.modeLabel = '執行中'; break;
      case 'COMPLETED':                this.modeLabel = '已完成'; break;
      case 'BLOCKED_ALREADY_EXECUTED': this.modeLabel = '已執行（封鎖）'; break;
      case 'BLOCKED_NO_PERMISSION':    this.modeLabel = '無執行權限'; break;
    }
  }

  /** 使用者點擊「確認執行」→ 打開 SP-07 modal */
  openExecuteConfirm(): void {
    this.showConfirmModal = true;
  }

  onConfirmModalConfirm(): void {
    this.showConfirmModal = false;
    this.mode = 'EXECUTING';
    this.modeLabel = '執行中';
    // TODO: 呼叫 samplingService.execute(batchId)
  }

  onConfirmModalCancel(): void {
    this.showConfirmModal = false;
  }
}

