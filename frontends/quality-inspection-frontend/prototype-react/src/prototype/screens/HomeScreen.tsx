import { Link } from 'react-router-dom';
import { AppShell } from '../components';
import { homeScenarios } from '../data';

export function HomeScreen() {
  return (
    <AppShell>
      <div className="page-stack">
        <section className="panel hero-panel">
          <p className="eyebrow">Reviewer hub</p>
          <h2>三頁原型與 flow validation 入口</h2>
          <p>
            這個 prototype 只處理 UI-010、UI-013、UI-014。所有畫面都用 mock state 驅動，不依賴後端。
          </p>
          <div className="chip-grid">
            <Link className="variant-chip active" to="/cases/CASE-240013/detail?variant=FULL">
              <strong>UI-010</strong>
              <span>案件詳情 control center</span>
            </Link>
            <Link className="variant-chip active" to="/cases/CASE-240013/revisions?variant=ALL">
              <strong>UI-014</strong>
              <span>revision 歷程與 compare</span>
            </Link>
            <Link className="variant-chip active" to="/cases/CASE-240013/rereview?variant=NEW_REREVIEW">
              <strong>UI-013</strong>
              <span>複審 command page</span>
            </Link>
          </div>
        </section>

        {homeScenarios.map((group) => (
          <section className="panel section-gap" key={group.title}>
            <div className="section-header">
              <h3>{group.title}</h3>
            </div>
            <div className="action-list">
              {group.items.map((item) => (
                <Link className="action-card default" key={item.label} to={item.to}>
                  <strong>{item.label}</strong>
                  <span>open preset</span>
                </Link>
              ))}
            </div>
          </section>
        ))}
      </div>
    </AppShell>
  );
}
