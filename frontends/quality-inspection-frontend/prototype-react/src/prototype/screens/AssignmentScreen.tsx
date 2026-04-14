import { useMemo, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import {
  ActionLinks,
  AssignmentPickerPreview,
  AppShell,
  CommandBar,
  PageFrame,
  PlaceholderRegion,
  SummaryGrid,
  VariantSwitcher,
} from '../components';
import { demoCaseId } from '../data';
import { assignmentPresets, assignmentVariants } from '../p2Data';
import type { CommandAction } from '../types';

export function AssignmentScreen() {
  const location = useLocation();
  const params = useParams();
  const variantId = new URLSearchParams(location.search).get('variant') ?? 'ASSIGN_INITIAL';
  const preset = assignmentPresets[variantId] ?? assignmentPresets.ASSIGN_INITIAL;
  const caseId = params.caseId ?? demoCaseId;
  const [lastResult, setLastResult] = useState('');
  const basePath = useMemo(() => `/cases/${caseId}/assign`, [caseId]);

  function runCommand(action: CommandAction) {
    setLastResult(action.resultMessage ?? `${action.label} 已執行。`);
  }

  return (
    <AppShell>
      <PageFrame
        pageId="UI-011"
        pageName="案件分派頁"
        modeStrip={preset.modeStrip}
        banner={preset.banner}
        summaryFields={preset.summaryFields}
        roleSummary={`${preset.role} / ${preset.projectionMode} / ${preset.caseStatus}`}
        reviewerNotes={preset.reviewerNotes}
      >
        <VariantSwitcher title="UI-011 variants" options={assignmentVariants} basePath={basePath} />

        <section className="panel section-gap">
          <div className="section-header">
            <h3>Assignment mode banner</h3>
            <p>{preset.assignmentMode}</p>
          </div>
          <p className="scenario-description">{preset.modeExplanation}</p>
        </section>

        <section className="panel split-layout section-gap">
          <div>
            <div className="section-header">
              <h3>Reviewer assignment section</h3>
            </div>
            <SummaryGrid fields={preset.reviewerSectionFields} />
            <AssignmentPickerPreview model={preset.reviewerPicker} />
          </div>
          <div>
            <div className="section-header">
              <h3>Rereviewer assignment section</h3>
            </div>
            <SummaryGrid fields={preset.rereviewerSectionFields} />
            <AssignmentPickerPreview model={preset.rereviewerPicker} />
          </div>
        </section>

        <PlaceholderRegion title="C23 due date helper placeholder" lines={preset.helperLines} />

        <CommandBar
          title="Assignment submit zone"
          description="page-level assignment commands，不是 A4 fixed-bottom review bar"
          actions={preset.commandActions}
          lastResult={lastResult}
          onRun={runCommand}
        />

        <ActionLinks title="Back / cancel navigation" actions={preset.navigateActions} />
      </PageFrame>
    </AppShell>
  );
}
