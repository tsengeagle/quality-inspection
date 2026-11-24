// Karma configuration file for unit/component tests.
// Baseline: Karma + Jasmine (Angular CLI default).

// D-077 第 4 點：瀏覽器以 UTC 時區執行，測試台北日期的換算不依執行環境的時區。
// 要在 module.exports 之前設定，啟動的 Chrome 才會繼承。只在 Linux 與 macOS 生效（CI 是 Linux）；
// Windows 的 Chrome 不讀 TZ，照系統時區執行。單元測試的預期值寫死 UTC 字串，所以在 Windows 也會通過。
process.env.TZ = 'UTC';

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {},
      clearContext: false
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/quality-inspection-frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' }
      ]
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['Chrome'],
    singleRun: false,
    restartOnFileChange: true
  });
};
