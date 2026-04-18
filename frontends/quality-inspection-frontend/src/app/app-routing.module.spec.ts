import { appRoutes } from './app-routing.module';

describe('app route policy rollout', () => {
  it('enables child-route guard evaluation for sampling and cases feature roots', () => {
    const samplingRoute = appRoutes.find(route => route.path === 'sampling');
    const casesRoute = appRoutes.find(route => route.path === 'cases');

    expect(samplingRoute?.canActivateChild?.length).toBe(3);
    expect(casesRoute?.canActivateChild?.length).toBe(3);
  });
});
