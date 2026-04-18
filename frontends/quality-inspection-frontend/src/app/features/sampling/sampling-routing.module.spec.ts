import { samplingRoutes } from './sampling-routing.module';

describe('sampling route policy rollout', () => {
  it('formalizes role policy for the batch list and execute routes', () => {
    const batchListRoute = samplingRoutes.find(route => route.path === 'batches');
    const executeRoute = samplingRoutes.find(route => route.path === ':projectId');

    expect(batchListRoute?.data).toEqual({
      roles: ['HOSPITAL_MANAGER', 'AUDITOR', 'SYSTEM_ADMIN']
    });
    expect(executeRoute?.data).toEqual({
      roles: ['HOSPITAL_MANAGER'],
      requiresHospitalScope: true
    });
  });
});
