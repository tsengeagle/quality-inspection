import { useMemo } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import {
  ActionLinks,
  AppShell,
  PageFrame,
  SummaryGrid,
  TimelinePanel,
  VariantSwitcher,
} from '../components';
import { demoCaseId, revisionPresets, revisionVariants } from '../data';

export function RevisionHistoryScreen() {
  const location = useLocation();
  const params = useParams();
  const variantId = new URLSearchParams(location.search).get('variant') ?? 'ALL';
  const preset = revisionPresets[variantId] ?? revisionPresets.ALL;
  const caseId = params.caseId ?? demoCaseId;
  const basePath = useMemo(() => `/cases/${caseId}/revisions`, [caseId]);

  return (
    <AppShell>
      <PageFrame
        pageId="UI-014"
        pageName="Revision 歷程頁"
        modeStrip={preset.modeStrip}
        banner={preset.banner}
        projectionNotice={preset.projectionNotice}
        summaryFields={preset.summaryFields}
        roleSummary={`${preset.role} / ${preset.projectionMode} / ${preset.caseStatus}`}
        reviewerNotes={preset.reviewerNotes}
      >
        <VariantSwitcher title="UI-014 variants" options={revisionVariants} basePath={basePath} />

        <section className="panel section-gap">
          <div className="section-header">
            <h3>Revision group tabs</h3>
            <p>tab mode: {preset.tabMode}</p>
          </div>
          <div className="chip-grid inline-mode-grid">
            <div className={`variant-chip ${preset.tabMode === 'ALL' ? 'active' : ''}`}>
              <strong>ALL</strong>
              <span>全部 stage</span>
            </div>
            <div className={`variant-chip ${preset.tabMode === 'REREVIEW_ONLY' ? 'active' : ''}`}>
              <strong>REREVIEW_ONLY</strong>
              <span>只看複審</span>
            </div>
          </div>
        </section>

        <TimelinePanel records={preset.timeline} />

        <section className="panel section-gap">
          <div className="section-header">
            <h3>Revision compare summary</h3>
            <p>{preset.compareMode}</p>
          </div>
          <SummaryGrid fields={preset.compareSummary} />
        </section>

        <ActionLinks title="Navigation action zone" actions={preset.navigateActions} />
      </PageFrame>
    </AppShell>
  );
}
