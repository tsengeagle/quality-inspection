import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  SamplingExecuteBlockedReason,
  SamplingExecuteContext,
  SamplingService
} from '../services/sampling.service';

export type SamplingExecuteMode =
  | 'READY_FOR_PREVIEW'
  | 'PREVIEW_COMPLETED'
  | 'EXECUTING'
  | 'EXECUTE_SUCCESS'
  | 'EXECUTE_FAILED'
  | 'READONLY_BLOCKED';

export type BlockedReason =
  | 'PROJECT_NOT_ACTIVE'
  | 'PROJECT_VERSION_NOT_PUBLISHED'
  | null;

@Component({
  selector: 'app-sampling-execute-page',
  templateUrl: './sampling-execute-page.component.html',
  styleUrls: ['./sampling-execute-page.component.scss']
})
export class SamplingExecutePageComponent implements OnInit {

  projectId = '';
  context: SamplingExecuteContext | null = null;
  private localPreviewStats: SamplingExecuteContext['previewStats'] = null;
  private executeFailureFallback: SamplingExecuteContext['postExecuteSummary'] | null = null;

  mode: SamplingExecuteMode = 'READY_FOR_PREVIEW';
  blockedReason: BlockedReason = null;
  modeLabel = '';

  showConfirmModal = false;

  get canPreview(): boolean {
    return this.mode === 'READY_FOR_PREVIEW' || this.mode === 'EXECUTE_FAILED';
  }

  get hasPreviewStats(): boolean {
    return !!this.context?.previewStats;
  }

  get showExecuteZone(): boolean {
    return this.mode === 'PREVIEW_COMPLETED' || this.mode === 'EXECUTING';
  }

  get showResultZone(): boolean {
    return !!this.context?.postExecuteSummary && (this.mode === 'EXECUTE_SUCCESS' || this.mode === 'EXECUTE_FAILED');
  }

  get previewActionLabel(): string {
    return this.mode === 'EXECUTE_FAILED' ? '重新預估母體' : '預估母體';
  }

  get previewActionHint(): string {
    if (this.mode === 'EXECUTE_FAILED') {
      return '先重新確認本次母體與排除規則，再決定是否再次執行。';
    }

    return '此步驟只做預估，不會建立正式抽選批次。';
  }

  get executeActionHint(): string {
    if (this.mode === 'EXECUTING') {
      return '系統正在建立正式抽選批次，請勿重複送出。';
    }

    return '確認預估結果後，再正式建立抽選批次。';
  }

  get resultZoneTitle(): string {
    if (this.mode === 'EXECUTE_SUCCESS') {
      return '執行結果';
    }

    if (this.mode === 'EXECUTE_FAILED') {
      return '執行狀態';
    }

    return '尚未執行';
  }

  get projectSummaryItems(): Array<{ label: string; value: string; emphasis?: boolean }> {
    if (!this.context) {
      return [];
    }

    return [
      { label: '專案編號', value: this.projectId },
      { label: '專案名稱', value: this.context.projectName || '尚未提供' , emphasis: true},
      { label: '院區', value: this.context.hospitalId || '尚未提供' },
      { label: '目前階段', value: this.modeLabel }
    ];
  }

  get stageCards(): Array<{ key: string; title: string; description: string; status: 'done' | 'active' | 'pending' | 'blocked' }> {
    const blocked = this.mode === 'READONLY_BLOCKED';
    const previewDone = this.mode === 'PREVIEW_COMPLETED' || this.mode === 'EXECUTING' || this.mode === 'EXECUTE_SUCCESS' || this.mode === 'EXECUTE_FAILED';
    const executeDone = this.mode === 'EXECUTE_SUCCESS';
    const executeFailed = this.mode === 'EXECUTE_FAILED';

    return [
      {
        key: 'preview',
        title: '1. 預估母體',
        description: '先確認母體、符合條件與排除件數。',
        status: blocked ? 'blocked' : previewDone ? 'done' : 'active'
      },
      {
        key: 'execute',
        title: '2. 正式執行',
        description: '建立正式抽選批次，保留本次執行結果。',
        status: blocked ? 'blocked' : executeDone || executeFailed ? 'done' : this.mode === 'PREVIEW_COMPLETED' || this.mode === 'EXECUTING' ? 'active' : 'pending'
      },
      {
        key: 'result',
        title: '3. 執行結果',
        description: '確認成功或失敗摘要，接續前往批次或案件列表。',
        status: blocked ? 'blocked' : executeDone || executeFailed ? 'active' : 'pending'
      }
    ];
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private samplingService: SamplingService
  ) {}

  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('projectId') ?? '';
    this.loadExecuteContext();
  }

  resolveMode(context: SamplingExecuteContext): void {
    if (context.blockedReason) {
      this.mode = 'READONLY_BLOCKED';
      this.blockedReason = context.blockedReason as BlockedReason;
    } else if (context.executing) {
      this.mode = 'EXECUTING';
      this.blockedReason = null;
    } else if (context.executeFailed) {
      this.mode = 'EXECUTE_FAILED';
      this.blockedReason = null;
    } else if (context.executedBatchId) {
      this.mode = 'EXECUTE_SUCCESS';
      this.blockedReason = null;
    } else if (context.previewReady) {
      this.mode = 'PREVIEW_COMPLETED';
      this.blockedReason = null;
    } else {
      this.mode = 'READY_FOR_PREVIEW';
      this.blockedReason = null;
    }

    this._applyMode();
  }

  private _applyMode(): void {
    switch (this.mode) {
      case 'READY_FOR_PREVIEW':
        this.modeLabel = '待預估';
        break;
      case 'PREVIEW_COMPLETED':
        this.modeLabel = '預估完成';
        break;
      case 'EXECUTING':
        this.modeLabel = '執行中';
        break;
      case 'EXECUTE_SUCCESS':
        this.modeLabel = '抽選完成';
        break;
      case 'EXECUTE_FAILED':
        this.modeLabel = '執行失敗';
        break;
      case 'READONLY_BLOCKED':
        this.modeLabel = '不可執行';
        break;
    }
  }

  openExecuteConfirm(): void {
    this.showConfirmModal = true;
  }

  requestPreview(): void {
    if (!this.context) {
      return;
    }
    this.samplingService.previewSampling(this.projectId).subscribe({
      next: stats => {
        this.localPreviewStats = stats;
        this.executeFailureFallback = null;
        this.context = {
          ...this.context!,
          previewReady: true,
          executeFailed: false,
          previewStats: stats,
          postExecuteSummary: null
        };
        this.resolveMode(this.context);
      }
    });
  }

  onConfirmModalConfirm(): void {
    this.showConfirmModal = false;
    this.mode = 'EXECUTING';
    this.modeLabel = '執行中';
    if (this.context) {
      this.context = {
        ...this.context,
        executing: true,
        executeFailed: false,
        postExecuteSummary: null
      };
    }

    this.samplingService.executeSampling(this.projectId).subscribe({
      next: () => {
        this.executeFailureFallback = null;
        this.loadExecuteContext();
      },
      error: () => {
        this.executeFailureFallback = {
          tone: 'failed',
          title: '抽選失敗',
          description: '抽選執行失敗，請重新預估後再試。',
          batchId: null
        };
        this.loadExecuteContext(true);
      }
    });
  }

  onConfirmModalCancel(): void {
    this.showConfirmModal = false;
  }

  navigateToBatchList(): void {
    this.router.navigate(['/sampling/batches']);
  }

  navigateToCases(): void {
    const batchId = this.context?.postExecuteSummary?.batchId;
    if (!batchId) {
      return;
    }

    this.router.navigate(['/cases'], {
      queryParams: { batchId }
    });
  }

  private loadExecuteContext(preserveLocalPreviewStats = false): void {
    if (!preserveLocalPreviewStats) {
      this.localPreviewStats = null;
    }

    this.samplingService.getExecuteContext(this.projectId).subscribe(context => {
      const previewStats = preserveLocalPreviewStats
        ? this.localPreviewStats ?? this.context?.previewStats ?? context.previewStats
        : context.previewStats;

      this.context = {
        ...context,
        previewStats,
        ...(this.executeFailureFallback
          ? {
              executeFailed: true,
              postExecuteSummary: this.executeFailureFallback
            }
          : {})
      };
      this.resolveMode(this.context);
    });
  }
}
