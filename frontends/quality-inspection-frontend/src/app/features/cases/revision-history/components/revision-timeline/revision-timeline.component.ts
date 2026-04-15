import { Component, Input } from '@angular/core';
import { RevisionHistoryItem } from '../../../services/cases.service';
import { RevisionStageTab } from '../../revision-history-page.component';

/** C20 — RevisionTimeline (UI-014)
 *  Presentational — 依 stageTab filter 顯示 revision nodes。 */
@Component({
  selector: 'app-revision-timeline',
  template: `
    <div class="revision-timeline">
      <div *ngIf="filteredRevisions.length === 0" class="empty-hint">目前沒有 revision 記錄。</div>
      <div
        *ngFor="let rev of filteredRevisions; let i = index"
        class="revision-node">
        <div class="node-dot"></div>
        <div class="node-content">
          <span class="version-label">{{ rev.stage }} 第 {{ rev.revisionNo }} 版</span>
          <span class="stage-badges">
            <span *ngIf="rev.isCurrentEffective" class="badge current">current effective</span>
            <span *ngIf="!rev.isCurrentEffective" class="badge superseded">superseded</span>
          </span>
          <span class="action-type">{{ rev.actionType }}</span>
          <span class="author">{{ rev.submittedBy }}</span>
          <span class="submitted-at">{{ rev.submittedAt }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .revision-timeline { padding: 16px; }
    .empty-hint { color: #999; font-size: 13px; }
    .revision-node { display: flex; gap: 12px; margin-bottom: 16px; }
    .node-dot { width: 10px; height: 10px; border-radius: 50%; background: #1565c0; margin-top: 4px; flex-shrink: 0; }
    .node-content { display: flex; flex-direction: column; gap: 2px; }
    .version-label { font-weight: 600; font-size: 14px; }
    .stage-badges { display: flex; gap: 8px; }
    .badge { font-size: 12px; text-transform: uppercase; }
    .badge.current { color: #2e7d32; }
    .badge.superseded { color: #8d6e63; }
    .action-type, .author, .submitted-at { font-size: 13px; color: #666; }
  `]
})
export class RevisionTimelineComponent {
  @Input() revisions: RevisionHistoryItem[] = [];
  @Input() stageTab: RevisionStageTab = 'ALL';

  get filteredRevisions(): RevisionHistoryItem[] {
    if (this.stageTab === 'INITIAL_ONLY') {
      return this.revisions.filter(r => r.stage === 'INITIAL');
    }
    if (this.stageTab === 'REREVIEW_ONLY') {
      return this.revisions.filter(r => r.stage === 'REREVIEW');
    }
    return this.revisions;
  }
}
