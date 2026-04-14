import { useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import {
  AppShell,
  CommandBar,
  PageFrame,
  PlaceholderRegion,
  SummaryGrid,
  VariantSwitcher,
} from '../components';
import { demoCaseId } from '../data';
import { initialReviewPresets, initialReviewVariants } from '../p2Data';
import type { CommandAction } from '../types';

export function InitialReviewScreen() {
  const location = useLocation();
  const params = useParams();
  const variantId = new URLSearchParams(location.search).get('variant') ?? 'NEW_INITIAL_REVIEW';
  const preset = initialReviewPresets[variantId] ?? initialReviewPresets.NEW_INITIAL_REVIEW;
  const caseId = params.caseId ?? demoCaseId;
  const [lastResult, setLastResult] = useState('');
  const basePath = useMemo(() => `/cases/${caseId}/initial-review`, [caseId]);

  function runCommand(action: CommandAction) {
    setLastResult(action.resultMessage ?? `${action.label} 已執行。`);
  }

  return (
    <AppShell>
      <PageFrame
        pageId="UI-012"
        pageName="初審作業頁"
        modeStrip={preset.modeStrip}
        banner={preset.banner}
        summaryFields={preset.summaryFields}
        roleSummary={`${preset.role} / ${preset.projectionMode} / ${preset.caseStatus}`}
        reviewerNotes={preset.reviewerNotes}
      >
        <VariantSwitcher title="UI-012 variants" options={initialReviewVariants} basePath={basePath} />

        <PlaceholderRegion title="初審評分表作答區" lines={preset.formOutline} />

        <section className="panel split-layout section-gap">
          <div>
            <div className="section-header">
              <h3>Revision / submission context</h3>
            </div>
            <SummaryGrid fields={preset.revisionContextFields} />
          </div>
          <div>
            <div className="section-header">
              <h3>初審版本摘要</h3>
            </div>
            <SummaryGrid fields={preset.versionFields} />
          </div>
        </section>

        <div className="inline-links">
          <Link to={`/cases/${caseId}/detail?variant=ROLE_LIMITED`}>返回 UI-010</Link>
          <Link to={`/cases/${caseId}/revisions?variant=ALL`}>查看 UI-014</Link>
        </div>

        <CommandBar
          title="Review command bar"
          description="A4 shared command zone，驗證 draft / submit / revision label grammar"
          actions={preset.commandActions}
          lastResult={lastResult}
          onRun={runCommand}
        />
      </PageFrame>
    </AppShell>
  );
}
