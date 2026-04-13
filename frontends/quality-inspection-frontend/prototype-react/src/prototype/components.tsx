import { Link, useLocation } from 'react-router-dom';
import type {
  CommandAction,
  CriticalFlowStep,
  LinkAction,
  ReviewScenarioGroup,
  SummaryField,
  TimelineRecord,
  VariantOption,
} from './types';

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-shell">
      <aside className="reviewer-hub">
        <div className="hub-block">
          <p className="eyebrow">P1 low-fidelity frontend prototype</p>
          <h1>Cases cluster flow validator</h1>
          <p className="lead">
            低保真、結構優先、以 variant 與 mock state 驗證 UI-010、UI-013、UI-014。
          </p>
          <nav className="hub-nav">
            <Link to="/">首頁</Link>
            <Link to="/review-harness">Review Harness</Link>
            <Link to="/cases/CASE-240013/detail?variant=FULL">UI-010</Link>
            <Link to="/cases/CASE-240013/revisions?variant=ALL">UI-014</Link>
            <Link to="/cases/CASE-240013/rereview?variant=NEW_REREVIEW">UI-013</Link>
          </nav>
        </div>
      </aside>
      <main className="content-area">{children}</main>
    </div>
  );
}

export function VariantSwitcher({
  title,
  options,
  basePath,
}: {
  title: string;
  options: VariantOption[];
  basePath: string;
}) {
  const location = useLocation();
  const current = new URLSearchParams(location.search).get('variant');

  return (
    <section className="panel section-gap">
      <div className="section-header">
        <h2>{title}</h2>
        <p>一鍵切換高風險 page mode / projection / flow。</p>
      </div>
      <div className="chip-grid">
        {options.map((option) => {
          const active = current === option.id;
          return (
            <Link
              key={option.id}
              className={`variant-chip ${active ? 'active' : ''}`}
              to={`${basePath}?variant=${option.id}`}
            >
              <strong>{option.label}</strong>
              <span>{option.description}</span>
            </Link>
          );
        })}
      </div>
    </section>
  );
}

export function PageFrame({
  pageId,
  pageName,
  modeStrip,
  banner,
  projectionNotice,
  summaryFields,
  roleSummary,
  reviewerNotes,
  children,
}: {
  pageId: string;
  pageName: string;
  modeStrip: string;
  banner?: { tone: 'info' | 'blocked' | 'exception'; title: string; detail: string };
  projectionNotice?: string;
  summaryFields: SummaryField[];
  roleSummary: string;
  reviewerNotes: string[];
  children: React.ReactNode;
}) {
  return (
    <div className="page-stack">
      <section className="panel page-header-panel">
        <div className="eyebrow-row">
          <span className="eyebrow">{pageId}</span>
          <span className="subtle">{roleSummary}</span>
        </div>
        <h2>{pageName}</h2>
        <SummaryGrid fields={summaryFields} compact={false} />
      </section>

      {projectionNotice ? (
        <section className="panel banner-panel projection">
          <strong>Projection notice</strong>
          <p>{projectionNotice}</p>
        </section>
      ) : null}

      {banner ? (
        <section className={`panel banner-panel ${banner.tone}`}>
          <strong>{banner.title}</strong>
          <p>{banner.detail}</p>
        </section>
      ) : null}

      <section className="panel mode-strip-panel">
        <span className="mode-label">mode strip</span>
        <strong>{modeStrip}</strong>
      </section>

      <section className="panel note-panel">
        <div className="section-header">
          <h3>Reviewer checkpoints</h3>
        </div>
        <ul className="flat-list">
          {reviewerNotes.map((note) => (
            <li key={note}>{note}</li>
          ))}
        </ul>
      </section>

      {children}
    </div>
  );
}

export function SummaryGrid({ fields, compact = true }: { fields: SummaryField[]; compact?: boolean }) {
  return (
    <div className={`summary-grid ${compact ? 'compact' : ''}`}>
      {fields.map((field) => (
        <div className={`summary-card ${field.muted ? 'muted' : ''}`} key={`${field.label}-${field.value}`}>
          <span>{field.label}</span>
          <strong>{field.value}</strong>
        </div>
      ))}
    </div>
  );
}

export function ActionLinks({ title, actions }: { title: string; actions: LinkAction[] }) {
  return (
    <section className="panel section-gap">
      <div className="section-header">
        <h3>{title}</h3>
        <p>navigate-only zone</p>
      </div>
      <div className="action-list">
        {actions.map((action) =>
          action.disabled ? (
            <div className="action-card disabled" key={action.label}>
              <strong>{action.label}</strong>
              <span>{action.reason ?? 'blocked'}</span>
            </div>
          ) : (
            <Link className={`action-card ${action.tone ?? 'default'}`} key={action.label} to={action.to ?? '#'}>
              <strong>{action.label}</strong>
              <span>{action.tone === 'warning' ? 'exception entry' : 'navigate'}</span>
            </Link>
          ),
        )}
      </div>
    </section>
  );
}

export function PlaceholderRegion({ title, lines }: { title: string; lines: string[] }) {
  return (
    <section className="panel section-gap dashed">
      <div className="section-header">
        <h3>{title}</h3>
        <p>placeholder only</p>
      </div>
      <ul className="flat-list">
        {lines.map((line) => (
          <li key={line}>{line}</li>
        ))}
      </ul>
    </section>
  );
}

export function TimelinePanel({ records }: { records: TimelineRecord[] }) {
  return (
    <section className="panel section-gap">
      <div className="section-header">
        <h3>Revision timeline</h3>
        <p>current effective 與 superseded 必須可辨識</p>
      </div>
      <div className="timeline-list">
        {records.map((record) => (
          <article className="timeline-item" key={record.id}>
            <div className="timeline-top">
              <strong>
                {record.stage} v{record.revisionNo}
              </strong>
              <span className={`status-pill ${record.isCurrentEffective ? 'current' : 'superseded'}`}>
                {record.isCurrentEffective ? 'Current' : 'Superseded'}
              </span>
            </div>
            <p>{record.submissionId}</p>
            <p>
              {record.submittedAt} / {record.submittedBy}
            </p>
            <p>{record.status}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

export function CommandBar({
  actions,
  lastResult,
  onRun,
}: {
  actions: CommandAction[];
  lastResult: string;
  onRun: (action: CommandAction) => void;
}) {
  return (
    <section className="panel section-gap command-panel">
      <div className="section-header">
        <h3>Command bar</h3>
        <p>UI-013 專用，與 navigate-only action zone 分離</p>
      </div>
      <div className="action-list">
        {actions.map((action) =>
          action.disabled ? (
            <div className="action-card disabled" key={action.label}>
              <strong>{action.label}</strong>
              <span>{action.reason ?? 'blocked'}</span>
            </div>
          ) : (
            <button
              className={`action-card button-card ${action.tone ?? 'default'}`}
              key={action.label}
              onClick={() => onRun(action)}
              type="button"
            >
              <strong>{action.label}</strong>
              <span>command</span>
            </button>
          ),
        )}
      </div>
      <div className="command-result">
        <strong>mock command result</strong>
        <p>{lastResult || '尚未執行 command。'}</p>
      </div>
    </section>
  );
}

export function RiskTagList({ tags }: { tags: string[] }) {
  return (
    <div className="risk-tag-list">
      {tags.map((tag) => (
        <span className="risk-tag" key={tag}>
          {tag}
        </span>
      ))}
    </div>
  );
}

export function ReviewScenarioSection({ group }: { group: ReviewScenarioGroup }) {
  return (
    <section className="panel section-gap">
      <div className="section-header section-header-stack">
        <div>
          <p className="eyebrow">{group.pageId}</p>
          <h3>{group.title}</h3>
        </div>
        <div className="section-copy">
          <p>{group.summary}</p>
          <p>
            {group.archetype} / {group.pageRole}
          </p>
        </div>
      </div>

      <div className="review-meta-grid">
        <article className="review-meta-card">
          <strong>High-risk targets</strong>
          <RiskTagList tags={group.highRiskTargets} />
        </article>
        <article className="review-meta-card">
          <strong>Shared structures</strong>
          <ul className="flat-list compact-list">
            {group.sharedStructures.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </article>
        <article className="review-meta-card deferred-card">
          <strong>Deferred / placeholder</strong>
          <ul className="flat-list compact-list">
            {group.deferredNotes.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </article>
      </div>

      <div className="scenario-grid">
        {group.scenarios.map((scenario) => (
          <article className="scenario-card" key={`${scenario.pageId}-${scenario.variantId}`}>
            <div className="scenario-top">
              <div>
                <p className="eyebrow">{scenario.pageId}</p>
                <h4>{scenario.label}</h4>
              </div>
              <RiskTagList tags={scenario.riskTags} />
            </div>
            <p className="scenario-description">{scenario.description}</p>
            <ul className="flat-list compact-list scenario-highlight-list">
              {scenario.highlights.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
            <Link className="action-card primary scenario-link" to={scenario.path}>
              <strong>開啟 scenario</strong>
              <span>{scenario.label}</span>
            </Link>
          </article>
        ))}
      </div>
    </section>
  );
}

export function CriticalFlowPanel({ steps }: { steps: CriticalFlowStep[] }) {
  return (
    <section className="panel section-gap">
      <div className="section-header section-header-stack">
        <div>
          <p className="eyebrow">Critical Flow</p>
          <h3>UI-010 → UI-014 → UI-013</h3>
        </div>
        <div className="section-copy">
          <p>固定 reviewer 驗證路徑，快速檢查 navigate-only 與 command bar 差異。</p>
        </div>
      </div>

      <div className="flow-grid">
        {steps.map((step, index) => (
          <article className="flow-card" key={step.title}>
            <span className="flow-index">0{index + 1}</span>
            <h4>{step.title}</h4>
            <p className="scenario-description">{step.pageRole}</p>
            <p className="scenario-description">{step.note}</p>
            <RiskTagList tags={step.riskTargets} />
            <Link className="action-card warning scenario-link" to={step.path}>
              <strong>進入此步驟</strong>
              <span>{step.title}</span>
            </Link>
          </article>
        ))}
      </div>
    </section>
  );
}
