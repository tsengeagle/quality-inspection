const ModuleFederationPlugin = require('webpack/lib/container/ModuleFederationPlugin');

/**
 * Module Federation configuration for microfrontend integration.
 *
 * In standalone mode, this config is not active — the app runs as a regular Angular app.
 * In integrated mode, this config exposes the app as a remote module
 * to the organization existing MFE shell.
 *
 * Reference: @angular-architects/module-federation
 *
 * NOTE: The actual remoteEntry name, shared dependencies, and exposed modules
 * should be aligned with the organization MFE shell's configuration.
 * Details pending confirmation from the organization shell team.
 */
module.exports = {
  name: 'qualityInspection',
  filename: 'remoteEntry.js',
  exposes: {
    './Module': './src/app/app.module.ts'
  },
  shared: {
    '@angular/core': { singleton: true, strictVersion: true, requiredVersion: '^14.3.0' },
    '@angular/common': { singleton: true, strictVersion: true, requiredVersion: '^14.3.0' },
    '@angular/router': { singleton: true, strictVersion: true, requiredVersion: '^14.3.0' },
    '@ngrx/store': { singleton: true, strictVersion: true, requiredVersion: '^14.3.0' },
    'rxjs': { singleton: true, strictVersion: false }
  }
};
