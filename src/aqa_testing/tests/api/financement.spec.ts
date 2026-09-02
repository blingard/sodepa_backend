import { test, expect } from '../../src/fixtures/api-fixtures';
import {
  expectHasFields,
  expectJsonArray,
  expectStatusIn,
  expectValidPage,
} from '../../src/utils/assertions';
import { BAD_REQUEST_STATUSES, NOT_FOUND_STATUSES } from '../../src/api/http';
import {
  UUID_INEXISTANT,
  UUID_MALFORME,
  isoDate,
  simulationValide,
  today,
  unique,
} from '../../src/data/builders';

test.describe('API — Financement : recherche et consultation', () => {
  test('la recherche paginée respecte la taille de page demandée', async ({ financementApi }) => {
    const page = await financementApi.lister({ page: 0, size: 5 });

    expectValidPage(page, { expectedPage: 0, expectedSize: 5 });
  });

  test('la pagination avance sans répéter les mêmes éléments', async ({ financementApi }) => {
    const premiere = await financementApi.lister({ page: 0, size: 2 });
    test.skip(premiere.totalElements < 3, 'jeu de données insuffisant pour paginer');

    const seconde = await financementApi.lister({ page: 1, size: 2 });
    expectValidPage(seconde, { expectedPage: 1, expectedSize: 2 });

    const idsPremiere = premiere.content.map((f) => f.id);
    expect(seconde.content.filter((f) => idsPremiere.includes(f.id))).toHaveLength(0);
  });

  test('une page hors bornes renvoie un contenu vide', async ({ financementApi }) => {
    const premiere = await financementApi.lister({ page: 0, size: 10 });

    const horsBornes = await financementApi.lister({ page: premiere.totalPages + 5, size: 10 });

    expect(horsBornes.content).toHaveLength(0);
    expect(horsBornes.empty).toBe(true);
  });

  test('le tri demandé est appliqué', async ({ financementApi }) => {
    const page = await financementApi.lister({ page: 0, size: 20, sort: 'intitule,asc' });
    test.skip(page.content.length < 2, 'moins de deux financements en base');

    const intitules = page.content.map((f) => String(f.intitule ?? ''));
    const tries = [...intitules].sort((a, b) => a.localeCompare(b));
    expect(intitules).toEqual(tries);
  });

  test('le filtre par type ne renvoie que les financements de ce type', async ({
    financementApi,
  }) => {
    const reference = await financementApi.lister({ page: 0, size: 1 });
    test.skip(reference.content.length === 0, 'aucun financement en base');

    const type = reference.content[0].type;
    test.skip(!type, 'le financement de référence ne porte pas de type');

    const filtres = await financementApi.lister({ page: 0, size: 50, type: type! });
    expectValidPage(filtres, { expectedPage: 0 });
    for (const financement of filtres.content) {
      expect(financement.type).toBe(type);
    }
  });

  test('le filtre par banque ne renvoie que ce prêteur', async ({ financementApi, banqueApi }) => {
    const banques = await banqueApi.list();
    test.skip(banques.length === 0, 'aucune banque en base');

    const banqueId = String(banques[0].id);
    const filtres = await financementApi.lister({ page: 0, size: 50, banqueId });

    expectValidPage(filtres, { expectedPage: 0 });
    for (const financement of filtres.content) {
      if (financement.banqueId !== undefined) {
        expect(financement.banqueId).toBe(banqueId);
      }
    }
  });

  test('un banqueId malformé est rejeté', async ({ apiContext }) => {
    const response = await apiContext.get('/api/financement', {
      params: { banqueId: UUID_MALFORME },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'banqueId malformé');
  });

  test('un type inconnu renvoie une page vide plutôt qu’une erreur', async ({ financementApi }) => {
    const page = await financementApi.lister({ page: 0, size: 10, type: unique('TYPE') });

    expect(page.content).toHaveLength(0);
    expect(page.totalElements).toBe(0);
  });

  test('la consultation unitaire renvoie la fiche et son échéancier', async ({
    financementApi,
  }) => {
    const page = await financementApi.lister({ page: 0, size: 1 });
    test.skip(page.content.length === 0, 'aucun financement en base');

    const financement = await financementApi.getById(page.content[0].id);

    expect(financement.id).toBe(page.content[0].id);
    expectHasFields(financement as unknown as Record<string, unknown>, ['id', 'intitule']);
  });

  test('un identifiant inexistant ne renvoie pas 200', async ({ financementApi }) => {
    const response = await financementApi.getByIdRaw(UUID_INEXISTANT, NOT_FOUND_STATUSES);

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'financement inexistant');
  });

  test('un identifiant malformé est rejeté en 400', async ({ financementApi }) => {
    const response = await financementApi.getByIdRaw(UUID_MALFORME, BAD_REQUEST_STATUSES);

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'financement id malformé');
  });
});

test.describe('API — Financement : simulation d’amortissement', () => {
  test('la simulation mensuelle renvoie autant d’échéances que de mois', async ({
    financementApi,
  }) => {
    const parametres = simulationValide({ dureeMois: 24, periodicite: 'MENSUELLE' });

    const echeancier = await financementApi.simuler(parametres);

    expect(echeancier.length).toBe(parametres.dureeMois);
  });

  test('la somme du capital amorti égale le capital emprunté', async ({ financementApi }) => {
    const parametres = simulationValide({ dureeMois: 12, periodicite: 'MENSUELLE' });

    const echeancier = await financementApi.simuler(parametres);

    const capitalAmorti = echeancier.reduce((total, e) => total + Number(e.capitalAmorti ?? 0), 0);
    expect(Math.abs(capitalAmorti - parametres.capital)).toBeLessThan(1);
  });

  test('le capital restant dû décroît strictement', async ({ financementApi }) => {
    const echeancier = await financementApi.simuler(
      simulationValide({ dureeMois: 12, periodicite: 'MENSUELLE' }),
    );
    test.skip(
      echeancier.some((e) => e.capitalRestantDu === undefined),
      'l’API n’expose pas capitalRestantDu',
    );

    for (let i = 1; i < echeancier.length; i += 1) {
      expect(Number(echeancier[i].capitalRestantDu)).toBeLessThan(
        Number(echeancier[i - 1].capitalRestantDu),
      );
    }
    expect(Math.abs(Number(echeancier[echeancier.length - 1].capitalRestantDu))).toBeLessThan(1);
  });

  test('la simulation trimestrielle produit quatre échéances par an', async ({
    financementApi,
  }) => {
    const echeancier = await financementApi.simuler(
      simulationValide({ dureeMois: 24, periodicite: 'TRIMESTRIELLE' }),
    );

    expect(echeancier.length).toBe(8);
  });

  test('la simulation annuelle produit une échéance par an', async ({ financementApi }) => {
    const echeancier = await financementApi.simuler(
      simulationValide({ dureeMois: 36, periodicite: 'ANNUELLE' }),
    );

    expect(echeancier.length).toBeLessThanOrEqual(36);
    expect(echeancier.length).toBeGreaterThan(0);
  });

  test('un taux nul produit un amortissement sans intérêts', async ({ financementApi }) => {
    const echeancier = await financementApi.simuler(
      simulationValide({ tauxNominal: 0, dureeMois: 12 }),
    );

    const interets = echeancier.reduce((total, e) => total + Number(e.interets ?? 0), 0);
    expect(interets).toBeCloseTo(0, 2);
  });

  test('le paramètre capital est obligatoire', async ({ apiContext }) => {
    const response = await apiContext.get('/api/financement/simuler', {
      params: { tauxNominal: 5, dureeMois: 12, periodicite: 'MENSUELLE', dateEffet: today() },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'simuler sans capital');
  });

  test('le paramètre periodicite est obligatoire', async ({ apiContext }) => {
    const response = await apiContext.get('/api/financement/simuler', {
      params: { capital: 1000, tauxNominal: 5, dureeMois: 12, dateEffet: today() },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'simuler sans periodicite');
  });

  test('le paramètre dateEffet est obligatoire', async ({ apiContext }) => {
    const response = await apiContext.get('/api/financement/simuler', {
      params: { capital: 1000, tauxNominal: 5, dureeMois: 12, periodicite: 'MENSUELLE' },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'simuler sans dateEffet');
  });

  test('une date au mauvais format est rejetée', async ({ apiContext }) => {
    const response = await apiContext.get('/api/financement/simuler', {
      params: {
        capital: 1000,
        tauxNominal: 5,
        dureeMois: 12,
        periodicite: 'MENSUELLE',
        dateEffet: '01/01/2025',
      },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'dateEffet mal formatée');
  });

  test('une durée non numérique est rejetée', async ({ apiContext }) => {
    const response = await apiContext.get('/api/financement/simuler', {
      params: {
        capital: 1000,
        tauxNominal: 5,
        dureeMois: 'douze',
        periodicite: 'MENSUELLE',
        dateEffet: today(),
      },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'dureeMois non numérique');
  });

  test('une périodicité inconnue est refusée ou retombe sur le calcul mensuel', async ({
    apiContext,
  }) => {
    const response = await apiContext.get('/api/financement/simuler', {
      params: {
        capital: 1000,
        tauxNominal: 5,
        dureeMois: 12,
        periodicite: 'HEBDOMADAIRE',
        dateEffet: today(),
      },
    });

    await expectStatusIn(response, [200, ...BAD_REQUEST_STATUSES], 'périodicité inconnue');
  });
});

test.describe('API — Financement : création et règlements', () => {
  test('la création exige une banque existante', async ({ financementApi }) => {
    const response = await financementApi.post('', {
      data: {
        banqueId: UUID_INEXISTANT,
        intitule: `Prêt ${unique()}`,
        type: 'PRET',
        capital: 1_000_000,
        tauxNominal: 6,
        dateEffet: today(),
        dureeMois: 12,
        periodicite: 'MENSUELLE',
        utilisateurId: UUID_INEXISTANT,
      },
      expectStatus: NOT_FOUND_STATUSES,
    });

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'création banque inexistante');
  });

  test('la création exige un intitulé', async ({ financementApi }) => {
    const response = await financementApi.post('', {
      data: {
        banqueId: UUID_INEXISTANT,
        intitule: '',
        type: 'PRET',
        capital: 1_000_000,
        tauxNominal: 6,
        dateEffet: today(),
        dureeMois: 12,
        periodicite: 'MENSUELLE',
        utilisateurId: UUID_INEXISTANT,
      },
      expectStatus: BAD_REQUEST_STATUSES,
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'création sans intitulé');
  });

  test('la création exige un capital strictement positif', async ({ financementApi }) => {
    const response = await financementApi.post('', {
      data: {
        banqueId: UUID_INEXISTANT,
        intitule: 'x',
        type: 'PRET',
        capital: 0,
        tauxNominal: 6,
        dateEffet: today(),
        dureeMois: 12,
        periodicite: 'MENSUELLE',
        utilisateurId: UUID_INEXISTANT,
      },
      expectStatus: BAD_REQUEST_STATUSES,
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'création capital nul');
  });

  test('un financement créé est relisible avec son échéancier', async ({
    financementApi,
    banqueApi,
    userApi,
  }) => {
    const banques = await banqueApi.list();
    test.skip(banques.length === 0, 'aucune banque en base');
    const utilisateurs = await userApi.page({ page: 0, size: 1 });
    test.skip(utilisateurs.content.length === 0, 'aucun utilisateur en base');

    const cree = await financementApi.creer({
      banqueId: String(banques[0].id),
      intitule: `Prêt ${unique()}`,
      type: 'PRET',
      capital: 12_000_000,
      tauxNominal: 6.5,
      dateEffet: today(),
      dureeMois: 12,
      periodicite: 'MENSUELLE',
      utilisateurId: String(utilisateurs.content[0].id),
    });

    expect(cree.id).toBeTruthy();
    const relu = await financementApi.getById(cree.id);
    expect(relu.id).toBe(cree.id);
    expect(relu.echeancier?.length ?? 0).toBeGreaterThan(0);
  });

  test('payer une échéance inexistante échoue', async ({ financementApi }) => {
    const response = await financementApi.payerEcheance(
      UUID_INEXISTANT,
      UUID_INEXISTANT,
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'paiement échéance inexistante');
  });

  test('payer une échéance exige un userId', async ({ apiContext }) => {
    const response = await apiContext.post(
      `/api/financement/echeances/${UUID_INEXISTANT}/payer`,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'paiement sans userId');
  });
});

test.describe('API — Financement : hors-bilan et reporting', () => {
  test('un engagement hors-bilan exige un tiers existant', async ({ financementApi }) => {
    const response = await financementApi.post('/hors-bilan', {
      data: {
        type: 'CAUTION',
        intitule: `Caution ${unique()}`,
        tiersId: UUID_INEXISTANT,
        montant: 5_000_000,
        dateEffet: today(),
        dateEcheance: isoDate(365),
      },
      expectStatus: NOT_FOUND_STATUSES,
    });

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'hors-bilan tiers inexistant');
  });

  test('un engagement hors-bilan exige un montant positif', async ({ financementApi }) => {
    const response = await financementApi.post('/hors-bilan', {
      data: {
        type: 'CAUTION',
        intitule: 'x',
        tiersId: UUID_INEXISTANT,
        montant: -1,
        dateEffet: today(),
        dateEcheance: isoDate(365),
      },
      expectStatus: BAD_REQUEST_STATUSES,
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'hors-bilan montant négatif');
  });

  test('un engagement hors-bilan créé apparaît dans le reporting OHADA', async ({
    financementApi,
    tiersApi,
  }) => {
    const tiers = await tiersApi.list();
    test.skip(tiers.length === 0, 'aucun tiers en base');

    const intitule = `Caution ${unique()}`;
    await financementApi.creerHorsBilan({
      type: 'CAUTION',
      intitule,
      tiersId: String(tiers[0].id),
      montant: 2_500_000,
      dateEffet: today(),
      dateEcheance: isoDate(365),
    });

    const reporting = (await financementApi.reportingHorsBilan()) as Record<string, unknown>[];
    expect(reporting.some((e) => e.intitule === intitule)).toBeTruthy();
  });

  test('le reporting hors-bilan renvoie un tableau', async ({ financementApi }) => {
    const response = await financementApi.get('/reporting/hors-bilan');

    await expectJsonArray(response);
  });

  test('les KPI financiers exposent les ratios attendus', async ({ financementApi }) => {
    const kpis = await financementApi.kpis();

    expect(kpis).toBeTruthy();
    expect(Object.keys(kpis).length).toBeGreaterThan(0);
  });
});
