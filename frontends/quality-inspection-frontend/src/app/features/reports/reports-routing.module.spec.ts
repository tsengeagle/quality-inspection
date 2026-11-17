import { appRoutes } from '../../app-routing.module';
import { ProjectionGuard } from '../../guards/projection.guard';
import { RoleGuard } from '../../guards/role.guard';
import { ScopeGuard } from '../../guards/scope.guard';
import { ReportExportPageComponent } from './report-export/report-export-page.component';
import { reportsRoutes } from './reports-routing.module';

// UI-017 §1：/reports/export，只允許 HOSPITAL_MANAGER、AUDITOR、SYSTEM_ADMIN。
describe('reports routes', () => {
  it('lazy-loads /reports behind the same guards as the other features', () => {
    const route = appRoutes.find(r => r.path === 'reports');
    expect(route?.loadChildren).toBeDefined();
    expect(route?.canActivate).toEqual([RoleGuard, ScopeGuard, ProjectionGuard]);
    expect(route?.canActivateChild).toEqual([RoleGuard, ScopeGuard, ProjectionGuard]);
    expect(appRoutes.indexOf(route!)).toBeLessThan(appRoutes.findIndex(r => r.path === '**'));
  });

  it('maps export to UI-017 for the three report roles only', () => {
    const route = reportsRoutes.find(r => r.path === 'export');
    expect(route?.component).toBe(ReportExportPageComponent);
    expect(route?.data?.['roles']).toEqual(['HOSPITAL_MANAGER', 'AUDITOR', 'SYSTEM_ADMIN']);
    expect(route?.data?.['roles']).not.toContain('REVIEWER');
    expect(route?.data?.['roles']).not.toContain('REREVIEWER');
  });
});
