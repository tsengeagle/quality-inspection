import { casesRoutes } from './cases-routing.module';

describe('cases route policy rollout', () => {
  it('formalizes manager-only assignment access with hospital scope requirements', () => {
    const assignmentRoute = casesRoutes.find(route => route.path === ':caseId/assign');

    expect(assignmentRoute?.data).toEqual({
      roles: ['HOSPITAL_MANAGER'],
      requiresHospitalScope: true
    });
  });
});
