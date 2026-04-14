import { Link } from 'react-router-dom';
import {
  AppShell,
  CriticalFlowPanel,
  ReviewScenarioSection,
} from '../components';
import {
  criticalFlowSteps,
  harnessDeferredNotes,
  harnessSharedStructureNotes,
  reviewHarnessGroups,
} from '../data';
import {
  p2HarnessDeferredNotes,
  p2HarnessSharedStructureNotes,
  p2ReviewHarnessGroups,
} from '../p2Data';

export function HomeScreen() {
  return (
    <AppShell>
      <div className="page-stack">
        <section className="panel hero-panel">
          <p className="eyebrow">Review harness</p>
          <h2>P1 + P2 scenario index / review harness</h2>
          <p>
            這個 review harness 固定 P1 與 P2 的 page role、shared structure、mode、projection 與
            command / navigate 邊界。reviewer 不需要手動拼 URL 或猜 variant。
          </p>
          <div className="hero-actions">
            <Link className="action-card primary scenario-link" to="/review-harness">
              <strong>固定 review harness 入口</strong>
              <span>/review-harness</span>
            </Link>
          </div>
          <div className="review-meta-grid harness-intro-grid">
            <article className="review-meta-card">
              <strong>Shared structures</strong>
              <ul className="flat-list compact-list">
                {[...harnessSharedStructureNotes, ...p2HarnessSharedStructureNotes].map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </article>
            <article className="review-meta-card deferred-card">
              <strong>Deferred / placeholder</strong>
              <ul className="flat-list compact-list">
                {[...harnessDeferredNotes, ...p2HarnessDeferredNotes].map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </article>
          </div>
        </section>

        <CriticalFlowPanel steps={criticalFlowSteps} />

        {reviewHarnessGroups.map((group) => (
          <ReviewScenarioSection group={group} key={group.pageId} />
        ))}

        {p2ReviewHarnessGroups.map((group) => (
          <ReviewScenarioSection group={group} key={group.pageId} />
        ))}
      </div>
    </AppShell>
  );
}
