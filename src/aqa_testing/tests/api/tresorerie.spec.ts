import { test, expect } from '../../src/fixtures/api-fixtures';
import { expectStatusIn } from '../../src/utils/assertions';
import { BAD_REQUEST_STATUSES, NOT_FOUND_STATUSES } from '../../src/api/http';
import {
  ANNEE_COURANTE,
  UUID_INEXISTANT,
  couvertureValide,
  debutAnnee,
  finAnnee,
  isoDate,
  previsionValide,
  today,
  unique,
} from '../../src/data/builders';

test.describe('API — Trésorerie : prévisions', () => {
  test('la liste des prévisions sur une période renvoie un tableau', async ({ tresorerieApi }) => {
    const previsions = await tresorerieApi.listerPrevisions(
      debutAnnee(ANNEE_COURANTE),
      finAnnee(ANNEE_COURANTE),
    );

    expect(Array.isArray(previsions)).toBeTruthy();
  });

  test('les prévisions renvoyées sont dans la fenêtre demandée', async ({ tresorerieApi }) => {
    const debut = debutAnnee(ANNEE_COURANTE);
    const fin = finAnnee(ANNEE_COURANTE);

    const previsions = await tresorerieApi.listerPrevisions(debut, fin);

    for (const prevision of previsions) {
      const echeance = prevision.dateEcheance as string | undefined;
      if (!echeance) continue;
      expect(echeance >= debut && echeance <= fin).toBeTruthy();
    }
  });

  test('une fenêtre inversée renvoie un résultat vide', async ({ tresorerieApi }) => {
    const previsions = await tresorerieApi.listerPrevisions(
      finAnnee(ANNEE_COURANTE),
      debutAnnee(ANNEE_COURANTE),
    );

    expect(previsions).toHaveLength(0);
  });

  test('le paramètre debut est obligatoire', async ({ apiContext }) => {
    const response = await apiContext.get('/api/tresorerie/previsions', {
      params: { fin: finAnnee(ANNEE_COURANTE) },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'previsions sans debut');
  });

  test('une date mal formatée est rejetée', async ({ apiContext }) => {
    const response = await apiContext.get('/api/tresorerie/previsions', {
      params: { debut: '2025/01/01', fin: finAnnee(ANNEE_COURANTE) },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'date mal formatée');
  });

  test('une prévision créée est retrouvée dans la période', async ({ tresorerieApi }) => {
    const libelle = `Prévision ${unique('PRV')}`;
    await tresorerieApi.ajouterPrevision(previsionValide({ libelle }));

    const previsions = await tresorerieApi.listerPrevisions(today(), isoDate(365));

    expect(previsions.some((p) => p.libelle === libelle)).toBeTruthy();
  });

  test('la création exige un montant strictement positif', async ({ tresorerieApi }) => {
    const response = await tresorerieApi.ajouterPrevision(
      previsionValide({ montant: 0 }),
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'prévision montant nul');
  });

  test('la création exige un libellé', async ({ tresorerieApi }) => {
    const response = await tresorerieApi.ajouterPrevision(
      previsionValide({ libelle: '' }),
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'prévision sans libellé');
  });

  test('la création exige une date d’échéance', async ({ tresorerieApi }) => {
    const response = await tresorerieApi.ajouterPrevision(
      previsionValide({ dateEcheance: undefined }),
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'prévision sans échéance');
  });
});

test.describe('API — Trésorerie : états et simulations', () => {
  test('le cash-flow mensuel couvre la période demandée', async ({ tresorerieApi }) => {
    const cashFlow = await tresorerieApi.cashFlow(
      debutAnnee(ANNEE_COURANTE),
      finAnnee(ANNEE_COURANTE),
    );

    expect(Array.isArray(cashFlow)).toBeTruthy();
    expect(cashFlow.length).toBeLessThanOrEqual(12);
  });

  test('le cash-flow exige les deux bornes', async ({ apiContext }) => {
    const response = await apiContext.get('/api/tresorerie/cash-flow', {
      params: { debut: debutAnnee(ANNEE_COURANTE) },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'cash-flow sans fin');
  });

  test('le BFR est calculé à une date donnée', async ({ tresorerieApi }) => {
    const bfr = await tresorerieApi.bfr(today());

    expect(bfr).toBeTruthy();
    expect(Object.keys(bfr).length).toBeGreaterThan(0);
  });

  test('le BFR exige une date', async ({ apiContext }) => {
    const response = await apiContext.get('/api/tresorerie/bfr');

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'bfr sans date');
  });

  test('les alertes de découvert renvoient un tableau', async ({ tresorerieApi }) => {
    const alertes = await tresorerieApi.alertesDecouvert();

    expect(Array.isArray(alertes)).toBeTruthy();
  });

  test('la simulation what-if renvoie un résultat', async ({ tresorerieApi }) => {
    const resultat = await tresorerieApi.whatIf(1.05, 1.03, 1.02);

    expect(resultat).toBeTruthy();
  });

  test('la simulation what-if exige ses trois paramètres', async ({ apiContext }) => {
    const response = await apiContext.get('/api/tresorerie/simulations/what-if', {
      params: { croissance: 1.05 },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'what-if incomplet');
  });

  test('un paramètre non numérique de la simulation est rejeté', async ({ apiContext }) => {
    const response = await apiContext.get('/api/tresorerie/simulations/what-if', {
      params: { croissance: 'beaucoup', inflation: 1, prixRevient: 1 },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'what-if non numérique');
  });
});

test.describe('API — Couverture de change (/api/tresorerie/change)', () => {
  test('la liste des couvertures renvoie un tableau', async ({ changeApi }) => {
    const couvertures = await changeApi.listerCouvertures();

    expect(Array.isArray(couvertures)).toBeTruthy();
  });

  test('le filtre par devise ne renvoie que cette devise', async ({ changeApi }) => {
    const couvertures = await changeApi.listerCouvertures({ devise: 'EUR' });

    for (const couverture of couvertures) {
      expect(couverture.devise).toBe('EUR');
    }
  });

  test('une devise inconnue renvoie une liste vide', async ({ changeApi }) => {
    const couvertures = await changeApi.listerCouvertures({ devise: 'ZZZ' });

    expect(couvertures).toHaveLength(0);
  });

  test('un contrat créé est retrouvé dans la liste', async ({ changeApi }) => {
    const reference = unique('CVT');
    await changeApi.enregistrerCouverture(couvertureValide({ reference }));

    const couvertures = await changeApi.listerCouvertures();

    expect(couvertures.some((c) => c.reference === reference)).toBeTruthy();
  });

  test('la création exige une référence', async ({ changeApi }) => {
    const response = await changeApi.enregistrerCouverture(
      couvertureValide({ reference: '' }),
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'couverture sans référence');
  });

  test('la création exige un montant positif', async ({ changeApi }) => {
    const response = await changeApi.enregistrerCouverture(
      couvertureValide({ montantDevise: 0 }),
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'couverture montant nul');
  });

  test('la création exige un cours garanti positif', async ({ changeApi }) => {
    const response = await changeApi.enregistrerCouverture(
      couvertureValide({ coursGaranti: -1 }),
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'couverture cours négatif');
  });

  test('l’évaluation d’un contrat inexistant échoue', async ({ changeApi }) => {
    const response = await changeApi.evaluer(UUID_INEXISTANT, 655.957, NOT_FOUND_STATUSES);

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'évaluation contrat inexistant');
  });

  test('l’évaluation exige un cours spot', async ({ apiContext }) => {
    const response = await apiContext.get(
      `/api/tresorerie/change/couverture/${UUID_INEXISTANT}/evaluer`,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'évaluation sans coursSpot');
  });

  test('l’évaluation d’un contrat existant produit une valorisation', async ({ changeApi }) => {
    const couvertures = await changeApi.listerCouvertures();
    test.skip(couvertures.length === 0, 'aucun contrat de couverture en base');

    const response = await changeApi.evaluer(String(couvertures[0].id), 655.957);

    expect(response.ok()).toBeTruthy();
  });
});

test.describe('API — Rapprochement et arbitrage (/api/tresorerie/rapprochement)', () => {
  test('le matching d’un relevé inexistant échoue', async ({ rapprochementBancaireApi }) => {
    const response = await rapprochementBancaireApi.matcher(UUID_INEXISTANT, NOT_FOUND_STATUSES);

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'matching relevé inexistant');
  });

  test('le matching exige un releveId', async ({ apiContext }) => {
    const response = await apiContext.post('/api/tresorerie/rapprochement/matching');

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'matching sans releveId');
  });

  test('l’arbitrage renvoie des recommandations', async ({ rapprochementBancaireApi }) => {
    const recommandations = await rapprochementBancaireApi.arbitrage(
      1_000_000,
      today(),
      isoDate(90),
      5_000_000,
    );

    expect(Array.isArray(recommandations)).toBeTruthy();
  });

  test('l’arbitrage exige ses quatre paramètres', async ({ apiContext }) => {
    const response = await apiContext.get('/api/tresorerie/rapprochement/arbitrage', {
      params: { fondsSecurite: 1000 },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'arbitrage incomplet');
  });
});

test.describe('API — Pilotage stratégique (/api/reporting)', () => {
  test('le TFT OHADA est généré pour un exercice', async ({ pilotageApi }) => {
    const tft = await pilotageApi.tft(ANNEE_COURANTE);

    expect(tft).toBeTruthy();
  });

  test('le TFT exige une année', async ({ apiContext }) => {
    const response = await apiContext.get('/api/reporting/tft');

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'tft sans année');
  });

  test('une année non numérique est rejetée', async ({ apiContext }) => {
    const response = await apiContext.get('/api/reporting/tft', { params: { annee: 'cette' } });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'tft année non numérique');
  });

  test('le runway est calculé', async ({ pilotageApi }) => {
    const runway = await pilotageApi.runway();

    expect(runway).toBeTruthy();
  });
});
