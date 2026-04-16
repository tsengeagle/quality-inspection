import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SamplingExecuteContext, SamplingService } from '../services/sampling.service';

export type SamplingExecuteMode =
  | 'READY_FOR_PREVIEW'
  | 'PREVIEW_COMPLETED'
  | 'EXECUTING'
  | 'EXECUTE_SUCCESS'
  | 'EXECUTE_FAILED'
  | 'READONLY_BLOCKED';

export type BlockedReason =
  | 'PROJECT_NOT_ACTIVE'
  | 'COMPLETED_BATCH_EXISTS'
  | 'PROJECT_CLOSED'
  | 'PERMISSION_REVOKED'
  | null;

@Component({
  selector: 'app-sampling-execute-page',
  templateUrl: './sampling-execute-page.component.html',
  styleUrls: ['./sampling-execute-page.component.scss']
})
export class SamplingExecutePageComponent implements OnInit {

  projectId = '';
  context: SamplingExecuteContext | null = null;

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
    this.samplingService.getExecuteContext(this.projectId).subscribe(context => {
      this.context = context;
      this.resolveMode(context);
    });
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

  openPreviewStub(): void {
    if (!this.context) {
      return;
    }
    // B-002: 呼叫真實 POST /review-projects/{projectId}/sampling/preview
    this.samplingService.previewSampling(this.projectId).subscribe({
      next: stats => {
        this.context = {
          ...this.context!,
          previewReady: true,
          previewStats: stats
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
      this.context = { ...this.context, executing: true };
    }

    // B-003: 呼叫真實 POST /review-projects/{projectId}/sampling/execute
    this.samplingService.executeSampling(this.projectId).subscribe({
      next: result => {
        this.context = {
          ...this.context!,
          executing: false,
          executedBatchId: result.batchId,
          postExecuteSummary: {
            tone: 'success',
            title: '抽選完成',
            description: `批次 ${result.batchId} 已完成抽選`,
            batchId: result.batchId
          }
        };
        this.resolveMode(this.context);
      },
      error: () => {
        this.context = {
          ...this.context!,
          executing: false,
          executeFailed: true
        };
        this.resolveMode(this.context!);
      }
    });
  }

  onConfirmModalCancel(): void {
    this.showConfirmModal = false;
  }

  navigateToBatchList(): void {
    this.router.navigate(['/sampling/batches']);
  }
}

