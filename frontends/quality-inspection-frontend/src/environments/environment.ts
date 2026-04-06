export const environment = {
  production: false,
  // Standalone mode: this app runs without the organization MFE shell.
  // In standalone mode, the app must simulate user context that the shell
  // would normally provide after login.
  standalone: true,
  apiBasePath: '/api/quality-inspection'
};
