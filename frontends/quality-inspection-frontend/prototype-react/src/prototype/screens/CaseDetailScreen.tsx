import { useMemo } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import {
  ActionLinks,
  AppShell,
  PageFrame,
  PlaceholderRegion,
  SummaryGrid,
  VariantSwitcher,
} from '../components';
import { caseDetailPresets, detailVariants, demoCaseId } from '../data';

export function CaseDetailScreen() {
  const location = useLocation();
  const params = useParams();
  const variantId = new URLSearchParams(location.search).get('variant') ?? 'FULL';
  const preset = caseDetailPresets[variantId] ?? caseDetailPresets.FULL;
  const caseId = params.caseId ?? demoCaseId;

  const basePath = useMemo(() => `/cases/${caseId}/detail`, [caseId]);

  return (
    <AppShell>
      <PageFrame
        pageId="UI-010"
        pageName="案件詳情頁"
        modeStrip={preset.modeStrip}
        banner={preset.banner}
        projectionNotice={preset.projectionNotice}
        summaryFields={preset.summaryFields}
        roleSummary={`${preset.role} / ${preset.projectionMode} / ${preset.caseStatus}`}
        reviewerNotes={preset.reviewerNotes}
      >
        <VariantSwitcher title="UI-010 variants" options={detailVariants} basePath={basePath} />
        <ActionLinks title="Status / action zone" actions={preset.navigateActions} />

        <section className="panel section-gap">
          <div className="section-header">
            <h3>Traceability summary</h3>
            <p>{preset.nextStepHint}</p>
          </div>
          <SummaryGrid fields={preset.traceabilityFields} />
        </section>

        <section className="panel split-layout section-gap">
          <div>
            <div className="section-header">
              <h3>Initial review summary</h3>
            </div>
            <SummaryGrid fields={preset.initialSummaryFields} />
          </div>
          <div>
            <div className="section-header">
              <h3>Rereview summary</h3>
            </div>
            <SummaryGrid fields={preset.rereviewSummaryFields} />
          </div>
        </section>

        <PlaceholderRegion
          title="Revision / lifecycle entry area"
          lines={[
            'deferred placeholder only',
            '不在本輪定稿 lifecycle 細節',
            '保持 UI-010 為 control center，而不是 full traceability page',
          ]}
        />
      </PageFrame>
    </AppShell>
  );
}
