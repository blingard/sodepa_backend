import { defineConfig, devices } from '@playwright/test';
import { env } from './src/config/env';

/**
 * Configuration Playwright du socle AQA Sodepa.
 *
 * Trois projets :
 *  - `api`          : tests REST purs contre le backend Spring (aucun navigateur).
 *  - `ui-setup`     : authentifie une fois l'UI et sérialise la session dans .auth/.
 *  - `ui-chromium`  : tests d'interface, réutilisant la session de `ui-setup`.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: env.isCI,
  retries: env.isCI ? 2 : 0,
  workers: env.isCI ? 2 : undefined,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
  ],
  use: {
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    ignoreHTTPSErrors: true,
    extraHTTPHeaders: { Accept: 'application/json' },
  },
  projects: [
    {
      name: 'api',
      testDir: './tests/api',
      use: {
        baseURL: env.apiBaseUrl,
      },
    },
    {
      name: 'ui-setup',
      testDir: './tests/setup',
      testMatch: /.*\.setup\.ts/,
      use: { baseURL: env.uiBaseUrl, headless: env.headless },
    },
    {
      name: 'ui-chromium',
      testDir: './tests/ui',
      dependencies: ['ui-setup'],
      use: {
        ...devices['Desktop Chrome'],
        baseURL: env.uiBaseUrl,
        headless: env.headless,
        storageState: '.auth/user.json',
        viewport: { width: 1440, height: 900 },
      },
    },
  ],
});
