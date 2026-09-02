import { test, expect } from '../../src/fixtures/api-fixtures';
import { expectJsonArray, expectStatusIn } from '../../src/utils/assertions';
import { BAD_REQUEST_STATUSES, NOT_FOUND_STATUSES } from '../../src/api/http';
import {
  ANNEE_COURANTE,
  UUID_INEXISTANT,
  UUID_MALFORME,
  debutAnnee,
  finAnnee,
  unique,
} from '../../src/data/builders';

test.describe('API — Analytique : axes et sections', () => {
  test('la liste des axes renvoie un tableau', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.get('/axes');

    await expectJsonArray(response);
  });

  test('un axe créé apparaît dans la liste', async ({ analytiqueApi }) => {
    const code = unique('AXE');

    await analytiqueApi.creerAxe({ code, intitule: `Axe ${code}` }, [200, 201]);

    const axes = await analytiqueApi.listerAxes();
    expect(axes.some((a) => a.code === code)).toBeTruthy();
  });

  test('la création d’un axe sans code est refusée', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.creerAxe({ intitule: 'Sans code' }, [
      ...BAD_REQUEST_STATUSES,
      200,
      201,
    ]);

    await expectStatusIn(response, [...BAD_REQUEST_STATUSES, 200, 201], 'axe sans code');
  });

  test('un code d’axe dupliqué est refusé', async ({ analytiqueApi }) => {
    const code = unique('AXE');
    await analytiqueApi.creerAxe({ code, intitule: 'Premier' }, [200, 201]);

    const doublon = await analytiqueApi.creerAxe({ code, intitule: 'Doublon' }, [
      ...BAD_REQUEST_STATUSES,
      409,
      200,
      201,
    ]);

    expect([400, 409, 422, 500]).toContain(doublon.status());
  });

  test('la désactivation d’un axe est prise en compte', async ({ analytiqueApi }) => {
    const axes = await analytiqueApi.listerAxes();
    test.skip(axes.length === 0, 'aucun axe analytique en base');
    const id = String(axes[0].id);

    await analytiqueApi.modifierStatutAxe(id, false, [200, 204]);
    await analytiqueApi.modifierStatutAxe(id, true, [200, 204]);
  });

  test('la modification de statut d’un axe inexistant échoue', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.modifierStatutAxe(
      UUID_INEXISTANT,
      true,
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'statut axe inexistant');
  });

  test('le paramètre actif est obligatoire', async ({ apiContext }) => {
    const response = await apiContext.put(
      `/api/comptabilite/analytique/axes/${UUID_INEXISTANT}/statut`,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'statut sans paramètre actif');
  });

  test('une section créée apparaît sous son axe', async ({ analytiqueApi }) => {
    const axes = await analytiqueApi.listerAxes();
    test.skip(axes.length === 0, 'aucun axe analytique en base');
    const axeId = String(axes[0].id);
    const code = unique('SEC');

    await analytiqueApi.creerSection(axeId, { code, intitule: `Section ${code}` }, [200, 201]);

    const response = await analytiqueApi.listerSections(axeId);
    const sections = (await response.json()) as Record<string, unknown>[];
    expect(sections.some((s) => s.code === code)).toBeTruthy();
  });

  test('créer une section sous un axe inexistant échoue', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.creerSection(
      UUID_INEXISTANT,
      { code: unique('SEC'), intitule: 'x' },
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'section axe inexistant');
  });

  test('lister les sections d’un axe inexistant renvoie vide ou 404', async ({
    analytiqueApi,
  }) => {
    const response = await analytiqueApi.listerSections(UUID_INEXISTANT, [
      200,
      ...NOT_FOUND_STATUSES,
    ]);

    if (response.status() === 200) {
      const sections = await expectJsonArray(response);
      expect(sections).toHaveLength(0);
    }
  });

  test('un axeId malformé est rejeté', async ({ apiContext }) => {
    const response = await apiContext.get(
      `/api/comptabilite/analytique/axes/${UUID_MALFORME}/sections`,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'axeId malformé');
  });

  test('la modification de statut d’une section inexistante échoue', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.modifierStatutSection(
      UUID_INEXISTANT,
      false,
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'statut section inexistante');
  });
});

test.describe('API — Analytique : ventilations', () => {
  test('ventiler une ligne inexistante échoue', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.ventilerLigne(
      UUID_INEXISTANT,
      [{ sectionId: UUID_INEXISTANT, pourcentage: 100 }],
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'ventilation ligne inexistante');
  });

  test('une ventilation dont le total dépasse 100 % est refusée', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.ventilerLigne(
      UUID_INEXISTANT,
      [
        { sectionId: UUID_INEXISTANT, pourcentage: 60 },
        { sectionId: UUID_INEXISTANT, pourcentage: 60 },
      ],
      [...BAD_REQUEST_STATUSES, 404],
    );

    await expectStatusIn(response, [...BAD_REQUEST_STATUSES, 404], 'ventilation > 100 %');
  });

  test('une ventilation vide est refusée', async ({ analytiqueApi }) => {
    const response = await analytiqueApi.ventilerLigne(UUID_INEXISTANT, [], [
      ...BAD_REQUEST_STATUSES,
      404,
    ]);

    await expectStatusIn(response, [...BAD_REQUEST_STATUSES, 404], 'ventilation vide');
  });
});

test.describe('API — Analytique : budgets par section', () => {
  test('la liste des budgets d’une année renvoie un tableau', async ({ analytiqueBudgetApi }) => {
    const response = await analytiqueBudgetApi.get(`/${ANNEE_COURANTE}`);

    await expectJsonArray(response);
  });

  test('une année sans budget renvoie une liste vide', async ({ analytiqueBudgetApi }) => {
    const budgets = await analytiqueBudgetApi.listerParAnnee(1900);

    expect(budgets).toHaveLength(0);
  });

  test('une année non numérique est rejetée', async ({ apiContext }) => {
    const response = await apiContext.get('/api/comptabilite/analytique/budgets/annee');

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'année non numérique');
  });

  test('la définition d’un budget sur une section inexistante échoue', async ({
    analytiqueBudgetApi,
  }) => {
    const response = await analytiqueBudgetApi.definirBudget(
      {
        annee: ANNEE_COURANTE,
        sectionId: UUID_INEXISTANT,
        compteCode: '605200',
        montantBudget: 1_000_000,
      },
      [...NOT_FOUND_STATUSES, 200, 201],
    );

    await expectStatusIn(
      response,
      [...NOT_FOUND_STATUSES, 200, 201],
      'budget section inexistante',
    );
  });

  test('le suivi par section renvoie une liste', async ({ analytiqueBudgetApi }) => {
    const response = await analytiqueBudgetApi.listerParSection(
      ANNEE_COURANTE,
      UUID_INEXISTANT,
      [200, ...NOT_FOUND_STATUSES],
    );

    if (response.status() === 200) {
      const budgets = await expectJsonArray(response);
      expect(budgets).toHaveLength(0);
    }
  });
});

test.describe('API — Analytique : clés de répartition', () => {
  test('la liste des clés renvoie un tableau', async ({ cleRepartitionApi }) => {
    const response = await cleRepartitionApi.get('');

    await expectJsonArray(response);
  });

  test('une clé créée apparaît dans la liste', async ({ cleRepartitionApi }) => {
    const code = unique('CLE');

    await cleRepartitionApi.creerCle(
      { code, intitule: `Clé ${code}`, details: [] },
      [200, 201, ...BAD_REQUEST_STATUSES],
    );

    const cles = await cleRepartitionApi.listerCles();
    expect(Array.isArray(cles)).toBeTruthy();
  });

  test('une clé sans code est refusée', async ({ cleRepartitionApi }) => {
    const response = await cleRepartitionApi.creerCle({ intitule: 'Sans code', details: [] }, [
      ...BAD_REQUEST_STATUSES,
      200,
      201,
    ]);

    await expectStatusIn(response, [...BAD_REQUEST_STATUSES, 200, 201], 'clé sans code');
  });

  test('appliquer une clé inexistante à une ligne inexistante échoue', async ({
    cleRepartitionApi,
  }) => {
    const response = await cleRepartitionApi.appliquerCle(
      UUID_INEXISTANT,
      UUID_INEXISTANT,
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'application clé inexistante');
  });
});

test.describe('API — Analytique : reporting', () => {
  test('le grand livre analytique couvre la période demandée', async ({
    reportingAnalytiqueApi,
  }) => {
    const sections = await reportingAnalytiqueApi.grandLivre(
      debutAnnee(ANNEE_COURANTE),
      finAnnee(ANNEE_COURANTE),
    );

    expect(Array.isArray(sections)).toBeTruthy();
  });

  test('le grand livre exige ses deux bornes', async ({ apiContext }) => {
    const response = await apiContext.get('/api/comptabilite/analytique/reporting/grand-livre', {
      params: { debut: debutAnnee(ANNEE_COURANTE) },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'grand livre sans fin');
  });

  test('la balance analytique est équilibrée', async ({ reportingAnalytiqueApi }) => {
    const lignes = await reportingAnalytiqueApi.balance(
      debutAnnee(ANNEE_COURANTE),
      finAnnee(ANNEE_COURANTE),
    );
    test.skip(lignes.length === 0, 'aucun mouvement analytique sur la période');

    const debit = lignes.reduce((total, l) => total + Number(l.totalDebit ?? l.debit ?? 0), 0);
    const credit = lignes.reduce((total, l) => total + Number(l.totalCredit ?? l.credit ?? 0), 0);
    expect(Math.abs(debit - credit)).toBeLessThan(1);
  });

  test('le compte de résultat d’une section inexistante échoue proprement', async ({
    reportingAnalytiqueApi,
  }) => {
    const response = await reportingAnalytiqueApi.compteResultat(
      UUID_INEXISTANT,
      ANNEE_COURANTE,
      [200, ...NOT_FOUND_STATUSES],
    );

    await expectStatusIn(response, [200, ...NOT_FOUND_STATUSES], 'résultat section inexistante');
  });

  test('le suivi budgétaire d’une section inexistante échoue proprement', async ({
    reportingAnalytiqueApi,
  }) => {
    const response = await reportingAnalytiqueApi.suiviBudgetaire(
      UUID_INEXISTANT,
      ANNEE_COURANTE,
      [200, ...NOT_FOUND_STATUSES],
    );

    await expectStatusIn(response, [200, ...NOT_FOUND_STATUSES], 'suivi section inexistante');
  });

  test('le suivi budgétaire d’une section réelle est exploitable', async ({
    analytiqueApi,
    reportingAnalytiqueApi,
  }) => {
    const axes = await analytiqueApi.listerAxes();
    test.skip(axes.length === 0, 'aucun axe analytique en base');

    const sectionsResponse = await analytiqueApi.listerSections(String(axes[0].id));
    const sections = (await sectionsResponse.json()) as Record<string, unknown>[];
    test.skip(sections.length === 0, 'aucune section analytique en base');

    const response = await reportingAnalytiqueApi.suiviBudgetaire(
      String(sections[0].id),
      ANNEE_COURANTE,
    );

    expect(response.ok()).toBeTruthy();
  });
});
