import { test as base, request as playwrightRequest, APIRequestContext } from '@playwright/test';
import { env } from '../config/env';
import { users, TestUser } from '../data/users';
import { logger } from '../utils/logger';
import { TokenResponse } from '../api/models/common';
import {
  AnalytiqueApiClient,
  AnalytiqueBudgetApiClient,
  AuditApiClient,
  AuditTrailApiClient,
  AuthApiClient,
  BanqueApiClient,
  BudgetApiClient,
  BudgetCollaboratifApiClient,
  ChangeHedgingApiClient,
  CleRepartitionApiClient,
  ClotureApiClient,
  CompteApiClient,
  EcritureApiClient,
  EngagementWorkflowApiClient,
  FinancementApiClient,
  ImmobilisationApiClient,
  JournalApiClient,
  PilotageApiClient,
  RapprochementApiClient,
  RapprochementBancaireApiClient,
  ReportingAnalytiqueApiClient,
  ReportingApiClient,
  TiersApiClient,
  TresorerieApiClient,
  UserApiClient,
} from '../api/clients';

/** Fixtures partagées par worker (le jeton est obtenu une seule fois). */
interface WorkerFixtures {
  session: TokenResponse;
}

/** Fixtures disponibles dans chaque test API. */
interface TestFixtures {
  /** Contexte HTTP authentifié (Bearer) sur API_BASE_URL. */
  apiContext: APIRequestContext;
  /** Contexte HTTP sans jeton, pour les tests d'accès refusé. */
  anonContext: APIRequestContext;

  authApi: AuthApiClient;
  anonAuthApi: AuthApiClient;
  auditApi: AuditApiClient;
  auditTrailApi: AuditTrailApiClient;

  budgetApi: BudgetApiClient;
  budgetCollaboratifApi: BudgetCollaboratifApiClient;
  engagementWorkflowApi: EngagementWorkflowApiClient;
  financementApi: FinancementApiClient;

  tresorerieApi: TresorerieApiClient;
  changeApi: ChangeHedgingApiClient;
  rapprochementBancaireApi: RapprochementBancaireApiClient;
  pilotageApi: PilotageApiClient;

  analytiqueApi: AnalytiqueApiClient;
  analytiqueBudgetApi: AnalytiqueBudgetApiClient;
  cleRepartitionApi: CleRepartitionApiClient;
  reportingAnalytiqueApi: ReportingAnalytiqueApiClient;

  banqueApi: BanqueApiClient;
  compteApi: CompteApiClient;
  tiersApi: TiersApiClient;
  journalApi: JournalApiClient;
  userApi: UserApiClient;

  ecritureApi: EcritureApiClient;
  immobilisationApi: ImmobilisationApiClient;
  clotureApi: ClotureApiClient;
  rapprochementApi: RapprochementApiClient;
  reportingApi: ReportingApiClient;
}

/** Ouvre une session sur /api/auth/login pour l'utilisateur donné. */
export async function login(user: TestUser): Promise<TokenResponse> {
  const context = await playwrightRequest.newContext({
    baseURL: env.apiBaseUrl,
    timeout: env.apiTimeoutMs,
  });
  try {
    const response = await context.post('/api/auth/login', {
      data: { username: user.username, password: user.password },
    });
    if (!response.ok()) {
      throw new Error(
        `Connexion impossible pour "${user.username}" (${response.status()}) : ${await response.text()}`,
      );
    }
    const token = (await response.json()) as TokenResponse;
    logger.info(`session ouverte pour ${user.username}`);
    return token;
  } finally {
    await context.dispose();
  }
}

export const test = base.extend<TestFixtures, WorkerFixtures>({
  session: [
    async ({}, use) => {
      await use(await login(users.admin));
    },
    { scope: 'worker' },
  ],

  apiContext: async ({ session }, use) => {
    const context = await playwrightRequest.newContext({
      baseURL: env.apiBaseUrl,
      timeout: env.apiTimeoutMs,
      extraHTTPHeaders: {
        Authorization: `Bearer ${session.access_token}`,
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    });
    await use(context);
    await context.dispose();
  },

  anonContext: async ({}, use) => {
    const context = await playwrightRequest.newContext({
      baseURL: env.apiBaseUrl,
      timeout: env.apiTimeoutMs,
      extraHTTPHeaders: { 'Content-Type': 'application/json', Accept: 'application/json' },
    });
    await use(context);
    await context.dispose();
  },

  authApi: async ({ apiContext }, use) => use(new AuthApiClient(apiContext)),
  anonAuthApi: async ({ anonContext }, use) => use(new AuthApiClient(anonContext)),
  auditApi: async ({ apiContext }, use) => use(new AuditApiClient(apiContext)),
  auditTrailApi: async ({ apiContext }, use) => use(new AuditTrailApiClient(apiContext)),

  budgetApi: async ({ apiContext }, use) => use(new BudgetApiClient(apiContext)),
  budgetCollaboratifApi: async ({ apiContext }, use) =>
    use(new BudgetCollaboratifApiClient(apiContext)),
  engagementWorkflowApi: async ({ apiContext }, use) =>
    use(new EngagementWorkflowApiClient(apiContext)),
  financementApi: async ({ apiContext }, use) => use(new FinancementApiClient(apiContext)),

  tresorerieApi: async ({ apiContext }, use) => use(new TresorerieApiClient(apiContext)),
  changeApi: async ({ apiContext }, use) => use(new ChangeHedgingApiClient(apiContext)),
  rapprochementBancaireApi: async ({ apiContext }, use) =>
    use(new RapprochementBancaireApiClient(apiContext)),
  pilotageApi: async ({ apiContext }, use) => use(new PilotageApiClient(apiContext)),

  analytiqueApi: async ({ apiContext }, use) => use(new AnalytiqueApiClient(apiContext)),
  analytiqueBudgetApi: async ({ apiContext }, use) =>
    use(new AnalytiqueBudgetApiClient(apiContext)),
  cleRepartitionApi: async ({ apiContext }, use) => use(new CleRepartitionApiClient(apiContext)),
  reportingAnalytiqueApi: async ({ apiContext }, use) =>
    use(new ReportingAnalytiqueApiClient(apiContext)),

  banqueApi: async ({ apiContext }, use) => use(new BanqueApiClient(apiContext)),
  compteApi: async ({ apiContext }, use) => use(new CompteApiClient(apiContext)),
  tiersApi: async ({ apiContext }, use) => use(new TiersApiClient(apiContext)),
  journalApi: async ({ apiContext }, use) => use(new JournalApiClient(apiContext)),
  userApi: async ({ apiContext }, use) => use(new UserApiClient(apiContext)),

  ecritureApi: async ({ apiContext }, use) => use(new EcritureApiClient(apiContext)),
  immobilisationApi: async ({ apiContext }, use) => use(new ImmobilisationApiClient(apiContext)),
  clotureApi: async ({ apiContext }, use) => use(new ClotureApiClient(apiContext)),
  rapprochementApi: async ({ apiContext }, use) => use(new RapprochementApiClient(apiContext)),
  reportingApi: async ({ apiContext }, use) => use(new ReportingApiClient(apiContext)),
});

export { expect } from '@playwright/test';
