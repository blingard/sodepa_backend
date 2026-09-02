import { test as base } from '@playwright/test';
import { LoginPage } from '../ui/pages/LoginPage';
import { DashboardPage } from '../ui/pages/DashboardPage';

/** Page Objects injectés dans les tests d'interface. */
interface UiFixtures {
  loginPage: LoginPage;
  dashboardPage: DashboardPage;
}

export const test = base.extend<UiFixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },
  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },
});

export { expect } from '@playwright/test';
