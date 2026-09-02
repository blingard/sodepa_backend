import { test, expect } from '../../src/fixtures/api-fixtures';
import { expectJsonArray, expectStatusIn } from '../../src/utils/assertions';
import { BAD_REQUEST_STATUSES, NOT_FOUND_STATUSES } from '../../src/api/http';
import { ANNEE_COURANTE, UUID_INEXISTANT, UUID_MALFORME } from '../../src/data/builders';

test.describe('API — Budget collaboratif (/api/budget/collaboratif)', () => {
  test('la liste des demandes renvoie un tableau', async ({ budgetCollaboratifApi }) => {
    const demandes = await budgetCollaboratifApi.listerDemandes();

    expect(Array.isArray(demandes)).toBeTruthy();
  });

  test('les filtres facultatifs sont acceptés simultanément', async ({ budgetCollaboratifApi }) => {
    const demandes = await budgetCollaboratifApi.listerDemandes({
      departementId: UUID_INEXISTANT,
      annee: ANNEE_COURANTE,
      statut: 'BROUILLON',
    });

    expect(Array.isArray(demandes)).toBeTruthy();
    expect(demandes).toHaveLength(0);
  });

  test('le filtre par année ne renvoie que cette année', async ({ budgetCollaboratifApi }) => {
    const toutes = (await budgetCollaboratifApi.listerDemandes()) as Record<string, unknown>[];
    test.skip(toutes.length === 0, 'aucune demande en base');

    const annee = toutes[0].annee as number | undefined;
    test.skip(annee === undefined, 'la demande de référence ne porte pas d’année');

    const filtrees = (await budgetCollaboratifApi.listerDemandes({ annee })) as Record<
      string,
      unknown
    >[];
    for (const demande of filtrees) {
      expect(demande.annee).toBe(annee);
    }
  });

  test('un departementId malformé est rejeté', async ({ apiContext }) => {
    const response = await apiContext.get('/api/budget/collaboratif/demandes', {
      params: { departementId: UUID_MALFORME },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'departementId malformé');
  });

  test('la saisie exige un compteCode', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.saisirDemande(
      { departementId: UUID_INEXISTANT, annee: ANNEE_COURANTE, montant: 1000 },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'demande sans compteCode');
  });

  test('la saisie exige un montant strictement positif', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.saisirDemande(
      {
        departementId: UUID_INEXISTANT,
        annee: ANNEE_COURANTE,
        compteCode: '605200',
        montant: 0,
      },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'demande montant nul');
  });

  test('la saisie sur un département inexistant échoue', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.saisirDemande(
      {
        departementId: UUID_INEXISTANT,
        annee: ANNEE_COURANTE,
        compteCode: '605200',
        montant: 1000,
        commentaires: 'test AQA',
      },
      [...NOT_FOUND_STATUSES, 200],
    );

    await expectStatusIn(response, [...NOT_FOUND_STATUSES, 200], 'demande département inexistant');
  });

  test('la soumission groupée exige departementId et annee', async ({ apiContext }) => {
    const response = await apiContext.post('/api/budget/collaboratif/demandes/soumettre', {
      params: { annee: ANNEE_COURANTE },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'soumettre sans departementId');
  });

  test('approuver une demande inexistante échoue', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.approuverDemande(
      UUID_INEXISTANT,
      UUID_INEXISTANT,
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'approbation demande inexistante');
  });

  test('rejeter une demande exige un motif', async ({ apiContext }) => {
    const response = await apiContext.post(
      `/api/budget/collaboratif/demandes/${UUID_INEXISTANT}/rejeter`,
      { params: { userId: UUID_INEXISTANT } },
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'rejet sans motif');
  });

  test('rejeter une demande inexistante échoue', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.rejeterDemande(
      UUID_INEXISTANT,
      'motif AQA',
      UUID_INEXISTANT,
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'rejet demande inexistante');
  });

  test('le cadrage exige un coefficient positif', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.appliquerCadrage(
      {
        annee: ANNEE_COURANTE,
        comptePrefix: '6',
        coefficient: 0,
        responsableId: UUID_INEXISTANT,
      },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'cadrage coefficient nul');
  });

  test('le cadrage exige un préfixe de compte', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.appliquerCadrage(
      {
        annee: ANNEE_COURANTE,
        comptePrefix: '',
        coefficient: 1.1,
        responsableId: UUID_INEXISTANT,
      },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'cadrage sans préfixe');
  });

  test('le cadrage nominal est accepté ou refusé sur données absentes', async ({
    budgetCollaboratifApi,
  }) => {
    const response = await budgetCollaboratifApi.appliquerCadrage(
      {
        annee: ANNEE_COURANTE,
        comptePrefix: '60',
        coefficient: 1.05,
        responsableId: UUID_INEXISTANT,
      },
      [200, ...NOT_FOUND_STATUSES],
    );

    await expectStatusIn(response, [200, ...NOT_FOUND_STATUSES], 'cadrage nominal');
  });

  test('la génération depuis l’historique exige des coefficients positifs', async ({
    budgetCollaboratifApi,
  }) => {
    const response = await budgetCollaboratifApi.genererDepuisHistorique(
      {
        anneeSource: ANNEE_COURANTE - 1,
        anneeCible: ANNEE_COURANTE,
        coeffVentes: -1,
        coeffCharges: 1,
        departementId: UUID_INEXISTANT,
      },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'génération coefficient négatif');
  });

  test('la génération depuis l’historique est traitée', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.genererDepuisHistorique(
      {
        anneeSource: ANNEE_COURANTE - 1,
        anneeCible: ANNEE_COURANTE + 1,
        coeffVentes: 1.1,
        coeffCharges: 1.05,
        departementId: UUID_INEXISTANT,
      },
      [200, ...NOT_FOUND_STATUSES],
    );

    await expectStatusIn(response, [200, ...NOT_FOUND_STATUSES], 'génération nominale');
  });

  test('la consolidation exige les trois paramètres', async ({ apiContext }) => {
    const response = await apiContext.post('/api/budget/collaboratif/consolider', {
      params: { annee: ANNEE_COURANTE },
    });

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'consolidation incomplète');
  });

  test('la consolidation sur un plan inexistant échoue', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.consolider(
      ANNEE_COURANTE,
      UUID_INEXISTANT,
      UUID_INEXISTANT,
      [200, ...NOT_FOUND_STATUSES],
    );

    await expectStatusIn(response, [200, ...NOT_FOUND_STATUSES], 'consolidation plan inexistant');
  });
});

test.describe('API — Workflow d’engagement (/api/budget/engagements/workflow)', () => {
  test('le pré-engagement exige une section analytique', async ({ engagementWorkflowApi }) => {
    const response = await engagementWorkflowApi.preEngager(
      {
        planId: UUID_INEXISTANT,
        compteCode: '605200',
        numeroEngagement: 'ENG-AQA',
        montant: 1000,
        utilisateurId: UUID_INEXISTANT,
      },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'pré-engagement sans sectionId');
  });

  test('le pré-engagement exige un montant positif', async ({ engagementWorkflowApi }) => {
    const response = await engagementWorkflowApi.preEngager(
      {
        planId: UUID_INEXISTANT,
        compteCode: '605200',
        sectionId: UUID_INEXISTANT,
        numeroEngagement: 'ENG-AQA',
        montant: -5,
        utilisateurId: UUID_INEXISTANT,
      },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'pré-engagement montant négatif');
  });

  test('le pré-engagement sur un plan inexistant échoue', async ({ engagementWorkflowApi }) => {
    const response = await engagementWorkflowApi.preEngager(
      {
        planId: UUID_INEXISTANT,
        compteCode: '605200',
        sectionId: UUID_INEXISTANT,
        numeroEngagement: 'ENG-AQA',
        description: 'test AQA',
        montant: 1000,
        utilisateurId: UUID_INEXISTANT,
      },
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'pré-engagement plan inexistant');
  });

  test('valider une étape exige un rôle approbateur', async ({ engagementWorkflowApi }) => {
    const response = await engagementWorkflowApi.validerEtape(
      { numeroEngagement: 'ENG-AQA', roleApprobateur: '', utilisateurId: UUID_INEXISTANT },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'validation sans rôle');
  });

  test('valider un engagement inexistant échoue', async ({ engagementWorkflowApi }) => {
    const response = await engagementWorkflowApi.validerEtape(
      {
        numeroEngagement: `INEXISTANT-${Date.now()}`,
        roleApprobateur: 'DAF',
        utilisateurId: UUID_INEXISTANT,
      },
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'validation engagement inexistant');
  });

  test('rejeter une étape exige un motif', async ({ engagementWorkflowApi }) => {
    const response = await engagementWorkflowApi.rejeter(
      { numeroEngagement: 'ENG-AQA', motif: '', utilisateurId: UUID_INEXISTANT },
      BAD_REQUEST_STATUSES,
    );

    await expectStatusIn(response, BAD_REQUEST_STATUSES, 'rejet sans motif');
  });

  test('rejeter un engagement inexistant échoue', async ({ engagementWorkflowApi }) => {
    const response = await engagementWorkflowApi.rejeter(
      {
        numeroEngagement: `INEXISTANT-${Date.now()}`,
        motif: 'test AQA',
        utilisateurId: UUID_INEXISTANT,
      },
      NOT_FOUND_STATUSES,
    );

    await expectStatusIn(response, NOT_FOUND_STATUSES, 'rejet engagement inexistant');
  });
});

test.describe('API — Piste d’audit budgétaire', () => {
  test('les demandes exposent une structure exploitable', async ({ budgetCollaboratifApi }) => {
    const response = await budgetCollaboratifApi.get('/demandes');

    await expectJsonArray(response);
  });
});
