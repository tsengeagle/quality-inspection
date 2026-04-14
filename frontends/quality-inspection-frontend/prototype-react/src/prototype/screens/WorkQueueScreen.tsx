import { useMemo } from 'react';
import { useLocation } from 'react-router-dom';
import {
  AppShell,
  FilterChipRow,
  PageFrame,
  VariantSwitcher,
  WorkQueueTable,
} from '../components';
import { workQueuePresets, workQueueVariants } from '../p2Data';

export function WorkQueueScreen() {
  const location = useLocation();
  const variantId = new URLSearchParams(location.search).get('variant') ?? 'MANAGER_DEFAULT_WORKLIST';
  const preset = workQueuePresets[variantId] ?? workQueuePresets.MANAGER_DEFAULT_WORKLIST;
  const basePath = useMemo(() => '/cases', []);

  return (
    <AppShell>
      <PageFrame
        pageId="UI-009"
        pageName="案件列表頁"
        modeStrip={`${preset.modeStrip} / tab=${preset.tabMode}`}
        banner={preset.banner}
        projectionNotice={preset.projectionNotice}
        projectionPlacement="inline"
        summaryFields={preset.summaryFields}
        roleSummary={`${preset.role} / ${preset.projectionMode} / ${preset.caseStatus}`}
        reviewerNotes={preset.reviewerNotes}
      >
        <VariantSwitcher title="UI-009 variants" options={workQueueVariants} basePath={basePath} />

        <section className="panel section-gap">
          <div className="section-header">
            <h3>Query preset / filters</h3>
            <p>{preset.nextStepHint}</p>
          </div>
          <FilterChipRow chips={preset.filterChips} />
        </section>

        {preset.rows.length ? (
          <WorkQueueTable rows={preset.rows} />
        ) : (
          <section className="panel section-gap dashed">
            <div className="section-header">
              <h3>Table-level state</h3>
              <p>forbidden / empty / error placeholder</p>
            </div>
            <p className="scenario-description">{preset.emptyMessage ?? '目前沒有 rows。'}</p>
          </section>
        )}
      </PageFrame>
    </AppShell>
  );
}
