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
          <span class="version-label">版本 {{ rev.version }}</span>
          <span class="action-type">{{ rev.actionType }}</span>
          <span class="author">{{ rev.authorName }}</span>
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
    .action-type, .author, .submitted-at { font-size: 13px; color: #666; }
  `]
})
export class RevisionTimelineComponent {
  @Input() revisions: RevisionHistoryItem[] = [];
  @Input() stageTab: RevisionStageTab = 'ALL';

  get filteredRevisions(): RevisionHistoryItem[] {
    if (this.stageTab === 'INITIAL_ONLY') {
      return this.revisions.filter(r => r.actionType.startsWith('INITIAL'));
    }
    if (this.stageTab === 'REREVIEW_ONLY') {
      return this.revisions.filter(r => r.actionType.startsWith('REREVIEW'));
    }
    return this.revisions;
  }
}
