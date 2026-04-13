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
import { demoCaseId, rereviewPresets, rereviewVariants } from '../data';
import type { CommandAction } from '../types';

export function RereviewScreen() {
  const location = useLocation();
  const params = useParams();
  const variantId = new URLSearchParams(location.search).get('variant') ?? 'NEW_REREVIEW';
  const preset = rereviewPresets[variantId] ?? rereviewPresets.NEW_REREVIEW;
  const caseId = params.caseId ?? demoCaseId;
  const [lastResult, setLastResult] = useState('');

  const basePath = useMemo(() => `/cases/${caseId}/rereview`, [caseId]);

  function runCommand(action: CommandAction) {
    setLastResult(action.resultMessage ?? `${action.label} 已執行。`);
  }

  return (
    <AppShell>
      <PageFrame
        pageId="UI-013"
        pageName="複審作業頁"
        modeStrip={preset.modeStrip}
        banner={preset.banner}
        projectionNotice={preset.projectionNotice}
        summaryFields={preset.summaryFields}
        roleSummary={`${preset.role} / ${preset.projectionMode} / ${preset.caseStatus}`}
        reviewerNotes={preset.reviewerNotes}
      >
        <VariantSwitcher title="UI-013 variants" options={rereviewVariants} basePath={basePath} />

        <section className="panel section-gap">
          <div className="section-header">
            <h3>初審摘要區</h3>
            <p>reference-only，複審前置資訊</p>
          </div>
          <SummaryGrid fields={preset.initialReferenceFields} />
          <div className="inline-links">
            <Link to={`/cases/${caseId}/detail?variant=CLOSED_REREVIEWER_HINT`}>返回 UI-010</Link>
            <Link to={`/cases/${caseId}/revisions?variant=REREVIEW_ONLY`}>查看 UI-014</Link>
          </div>
        </section>

        <PlaceholderRegion title="複審評分表作答區" lines={preset.formOutline} />

        <section className="panel split-layout section-gap">
          <div>
            <div className="section-header">
              <h3>最終分數 / 結案提示</h3>
            </div>
            <SummaryGrid fields={preset.decisionFields} />
          </div>
          <div>
            <div className="section-header">
              <h3>複審版本摘要</h3>
            </div>
            <SummaryGrid fields={preset.versionFields} />
          </div>
        </section>

        <CommandBar actions={preset.commandActions} lastResult={lastResult} onRun={runCommand} />
      </PageFrame>
    </AppShell>
  );
}
