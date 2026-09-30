const {defineConfig} = require('@playwright/test');
if (!process.env.E2E_BASE_URL) throw new Error('Set E2E_BASE_URL to the isolated validation backend');
module.exports = defineConfig({
  testDir: './tests/e2e',
  workers: 1,
  reporter: 'list',
  use: {baseURL: process.env.E2E_BASE_URL, channel: process.env.E2E_BROWSER || 'msedge',
    headless: true, screenshot: 'only-on-failure', trace: 'retain-on-failure'}
});
