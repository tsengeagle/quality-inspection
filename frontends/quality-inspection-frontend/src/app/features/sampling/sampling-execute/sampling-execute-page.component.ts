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
