/**
 * Inventaire des routes exposées par le backend.
 *
 * Source unique des tests transverses (sécurité, surface d'API) : toute route
 * ajoutée au backend doit apparaître ici, sinon `surface.spec.ts` le signale.
 */
import { HttpMethod } from './http';

export interface EndpointDescriptor {
  method: HttpMethod;
  path: string;
  /** Domaine fonctionnel, utilisé pour regrouper les rapports. */
  domain: string;
  /** Route accessible sans jeton (cf. SecurityConfig.PUBLIC_ENDPOINTS). */
  public?: boolean;
  /** Route modifiant l'état de façon difficilement réversible. */
  destructive?: boolean;
  /** Route attendant un corps multipart : non sollicitée par les tests génériques. */
  multipart?: boolean;
  /** Corps minimal permettant d'atteindre la route (tests de sécurité). */
  sampleBody?: unknown;
  /** Paramètres de requête minimaux. */
  sampleParams?: Record<string, string | number | boolean>;
}

export const UUID_ZERO = '00000000-0000-0000-0000-000000000000';
const DATE = '2025-01-01';

export const endpoints: EndpointDescriptor[] = [
  // --- Authentification -----------------------------------------------------
  { method: 'post', path: '/api/auth/login', domain: 'auth', public: true, sampleBody: { username: 'x', password: 'y' } },
  { method: 'post', path: '/api/auth/refresh', domain: 'auth', public: true, sampleBody: { refreshToken: 'x' } },
  { method: 'post', path: '/api/auth/logout', domain: 'auth', public: true, sampleBody: { refreshToken: 'x' } },
  { method: 'get', path: '/api/auth/sessions', domain: 'auth' },
  { method: 'delete', path: `/api/auth/sessions/${UUID_ZERO}`, domain: 'auth', destructive: true },
  { method: 'post', path: '/api/auth/change-password', domain: 'auth', destructive: true, sampleBody: { newPassword: 'Aa1!aaaa' } },

  // --- Audit ----------------------------------------------------------------
  { method: 'get', path: '/api/auth/audit/activities', domain: 'audit' },
  { method: 'get', path: '/api/auth/audit/clickhouse/transactions', domain: 'audit' },
  { method: 'get', path: '/api/auth/audit/clickhouse/activities', domain: 'audit' },
  { method: 'get', path: '/api/auth/audit/analytics', domain: 'audit', sampleParams: { query: 'SELECT 1' } },
  { method: 'get', path: '/api/audit/logs', domain: 'audit', sampleParams: { entiteNom: 'BudgetPlan', entiteId: UUID_ZERO } },

  // --- Budget ---------------------------------------------------------------
  { method: 'get', path: '/api/budget/plans', domain: 'budget' },
  { method: 'get', path: `/api/budget/plans/${UUID_ZERO}`, domain: 'budget' },
  { method: 'get', path: '/api/budget/engagements', domain: 'budget' },
  { method: 'get', path: '/api/budget/engagements/INEXISTANT', domain: 'budget' },
  { method: 'post', path: '/api/budget/plans', domain: 'budget', sampleBody: { annee: 2030, intitule: 'x', utilisateurId: UUID_ZERO } },
  { method: 'post', path: `/api/budget/plans/${UUID_ZERO}/items`, domain: 'budget', sampleBody: { compteCode: '605200', montant: 1 } },
  { method: 'post', path: `/api/budget/plans/${UUID_ZERO}/soumettre`, domain: 'budget', sampleParams: { userId: UUID_ZERO } },
  { method: 'post', path: `/api/budget/plans/${UUID_ZERO}/approuver`, domain: 'budget', sampleParams: { userId: UUID_ZERO } },
  { method: 'post', path: `/api/budget/plans/${UUID_ZERO}/rejeter`, domain: 'budget', sampleParams: { userId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/reallocations', domain: 'budget', sampleBody: { sourceItemId: UUID_ZERO, destItemId: UUID_ZERO, montant: 1, responsableId: UUID_ZERO, raison: 'x' } },
  { method: 'post', path: '/api/budget/engagements', domain: 'budget', sampleBody: { planId: UUID_ZERO, compteCode: '605200', numeroEngagement: 'x', description: 'x', montant: 1, utilisateurId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/engagements/X/liquider', domain: 'budget', sampleParams: { userId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/engagements/X/annuler', domain: 'budget', sampleParams: { userId: UUID_ZERO } },

  // --- Budget collaboratif --------------------------------------------------
  { method: 'get', path: '/api/budget/collaboratif/demandes', domain: 'budget-collaboratif' },
  { method: 'post', path: '/api/budget/collaboratif/demandes', domain: 'budget-collaboratif', sampleBody: { departementId: UUID_ZERO, annee: 2030, compteCode: '605200', montant: 1 } },
  { method: 'post', path: '/api/budget/collaboratif/demandes/soumettre', domain: 'budget-collaboratif', sampleParams: { departementId: UUID_ZERO, annee: 2030 } },
  { method: 'post', path: `/api/budget/collaboratif/demandes/${UUID_ZERO}/approuver`, domain: 'budget-collaboratif', sampleParams: { userId: UUID_ZERO } },
  { method: 'post', path: `/api/budget/collaboratif/demandes/${UUID_ZERO}/rejeter`, domain: 'budget-collaboratif', sampleParams: { motif: 'x', userId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/collaboratif/cadrage', domain: 'budget-collaboratif', sampleBody: { annee: 2030, comptePrefix: '6', coefficient: 1, responsableId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/collaboratif/generer', domain: 'budget-collaboratif', sampleBody: { anneeSource: 2029, anneeCible: 2030, coeffVentes: 1, coeffCharges: 1, departementId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/collaboratif/consolider', domain: 'budget-collaboratif', sampleParams: { annee: 2030, planId: UUID_ZERO, userId: UUID_ZERO } },

  // --- Workflow d'engagement ------------------------------------------------
  { method: 'post', path: '/api/budget/engagements/workflow/pre-engager', domain: 'engagement-workflow', sampleBody: { planId: UUID_ZERO, compteCode: '605200', sectionId: UUID_ZERO, numeroEngagement: 'x', montant: 1, utilisateurId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/engagements/workflow/valider', domain: 'engagement-workflow', sampleBody: { numeroEngagement: 'x', roleApprobateur: 'DAF', utilisateurId: UUID_ZERO } },
  { method: 'post', path: '/api/budget/engagements/workflow/rejeter', domain: 'engagement-workflow', sampleBody: { numeroEngagement: 'x', motif: 'x', utilisateurId: UUID_ZERO } },

  // --- Financement ----------------------------------------------------------
  { method: 'get', path: '/api/financement', domain: 'financement' },
  { method: 'get', path: `/api/financement/${UUID_ZERO}`, domain: 'financement' },
  { method: 'post', path: '/api/financement', domain: 'financement', sampleBody: { banqueId: UUID_ZERO, intitule: 'x', type: 'PRET', capital: 1, tauxNominal: 1, dateEffet: DATE, dureeMois: 12, periodicite: 'MENSUELLE', utilisateurId: UUID_ZERO } },
  { method: 'get', path: '/api/financement/simuler', domain: 'financement', sampleParams: { capital: 1000, tauxNominal: 5, dureeMois: 12, periodicite: 'MENSUELLE', dateEffet: DATE } },
  { method: 'post', path: `/api/financement/echeances/${UUID_ZERO}/payer`, domain: 'financement', sampleParams: { userId: UUID_ZERO } },
  { method: 'post', path: '/api/financement/hors-bilan', domain: 'financement', sampleBody: { type: 'CAUTION', intitule: 'x', tiersId: UUID_ZERO, montant: 1, dateEffet: DATE, dateEcheance: DATE } },
  { method: 'get', path: '/api/financement/reporting/hors-bilan', domain: 'financement' },
  { method: 'get', path: '/api/financement/reporting/kpis', domain: 'financement' },

  // --- Trésorerie -----------------------------------------------------------
  { method: 'get', path: '/api/tresorerie/previsions', domain: 'tresorerie', sampleParams: { debut: DATE, fin: DATE } },
  { method: 'post', path: '/api/tresorerie/previsions', domain: 'tresorerie', sampleBody: { dateEcheance: DATE, type: 'ENCAISSEMENT', source: 'MANUEL', libelle: 'x', montant: 1 } },
  { method: 'get', path: '/api/tresorerie/cash-flow', domain: 'tresorerie', sampleParams: { debut: DATE, fin: DATE } },
  { method: 'get', path: '/api/tresorerie/bfr', domain: 'tresorerie', sampleParams: { date: DATE } },
  { method: 'get', path: '/api/tresorerie/decouverts/alertes', domain: 'tresorerie' },
  { method: 'get', path: '/api/tresorerie/simulations/what-if', domain: 'tresorerie', sampleParams: { croissance: 1, inflation: 1, prixRevient: 1 } },
  { method: 'get', path: '/api/tresorerie/change/couverture', domain: 'tresorerie-change' },
  { method: 'post', path: '/api/tresorerie/change/couverture', domain: 'tresorerie-change', sampleBody: { reference: 'x', devise: 'EUR', montantDevise: 1, coursGaranti: 1, dateEffet: DATE, dateEcheance: DATE } },
  { method: 'get', path: `/api/tresorerie/change/couverture/${UUID_ZERO}/evaluer`, domain: 'tresorerie-change', sampleParams: { coursSpot: 655 } },
  { method: 'post', path: '/api/tresorerie/rapprochement/matching', domain: 'tresorerie-rapprochement', sampleParams: { releveId: UUID_ZERO } },
  { method: 'get', path: '/api/tresorerie/rapprochement/arbitrage', domain: 'tresorerie-rapprochement', sampleParams: { fondsSecurite: 1, debut: DATE, fin: DATE, soldeActuel: 1 } },

  // --- Pilotage stratégique -------------------------------------------------
  { method: 'get', path: '/api/reporting/tft', domain: 'pilotage', sampleParams: { annee: 2025 } },
  { method: 'get', path: '/api/reporting/runway', domain: 'pilotage' },

  // --- Comptabilité analytique ----------------------------------------------
  { method: 'post', path: '/api/comptabilite/analytique/axes', domain: 'analytique', sampleBody: { code: 'X', intitule: 'x' } },
  { method: 'get', path: '/api/comptabilite/analytique/axes', domain: 'analytique' },
  { method: 'put', path: `/api/comptabilite/analytique/axes/${UUID_ZERO}/statut`, domain: 'analytique', sampleParams: { actif: true } },
  { method: 'post', path: `/api/comptabilite/analytique/axes/${UUID_ZERO}/sections`, domain: 'analytique', sampleBody: { code: 'X', intitule: 'x' } },
  { method: 'get', path: `/api/comptabilite/analytique/axes/${UUID_ZERO}/sections`, domain: 'analytique' },
  { method: 'put', path: `/api/comptabilite/analytique/sections/${UUID_ZERO}/statut`, domain: 'analytique', sampleParams: { actif: true } },
  { method: 'post', path: `/api/comptabilite/analytique/lignes/${UUID_ZERO}/ventiler`, domain: 'analytique', sampleBody: [{ sectionId: UUID_ZERO, pourcentage: 100 }] },
  { method: 'post', path: '/api/comptabilite/analytique/budgets', domain: 'analytique-budget', sampleBody: { annee: 2030, sectionId: UUID_ZERO, compteCode: '605200', montantBudget: 1 } },
  { method: 'get', path: '/api/comptabilite/analytique/budgets/2030', domain: 'analytique-budget' },
  { method: 'get', path: `/api/comptabilite/analytique/budgets/2030/sections/${UUID_ZERO}`, domain: 'analytique-budget' },
  { method: 'post', path: '/api/comptabilite/analytique/cles', domain: 'analytique-cles', sampleBody: { code: 'X', intitule: 'x', details: [] } },
  { method: 'get', path: '/api/comptabilite/analytique/cles', domain: 'analytique-cles' },
  { method: 'post', path: `/api/comptabilite/analytique/cles/lignes/${UUID_ZERO}/appliquer/${UUID_ZERO}`, domain: 'analytique-cles' },
  { method: 'get', path: '/api/comptabilite/analytique/reporting/grand-livre', domain: 'analytique-reporting', sampleParams: { debut: DATE, fin: DATE } },
  { method: 'get', path: '/api/comptabilite/analytique/reporting/balance', domain: 'analytique-reporting', sampleParams: { debut: DATE, fin: DATE } },
  { method: 'get', path: `/api/comptabilite/analytique/reporting/sections/${UUID_ZERO}/resultat/2025`, domain: 'analytique-reporting' },
  { method: 'get', path: `/api/comptabilite/analytique/reporting/sections/${UUID_ZERO}/suivi-budgetaire/2025`, domain: 'analytique-reporting' },

  // --- Référentiel : banques ------------------------------------------------
  { method: 'post', path: '/api/v1/caccounting/bank/init_create', domain: 'banque', multipart: true },
  { method: 'get', path: '/api/v1/caccounting/bank', domain: 'banque' },
  { method: 'get', path: '/api/v1/caccounting/bank/list', domain: 'banque' },
  { method: 'get', path: `/api/v1/caccounting/bank/${UUID_ZERO}`, domain: 'banque' },
  { method: 'get', path: `/api/v1/caccounting/bank/active_by_id/${UUID_ZERO}`, domain: 'banque' },
  { method: 'put', path: `/api/v1/caccounting/bank/init_update/${UUID_ZERO}`, domain: 'banque', sampleBody: { code: 'X', name: 'x', accountingCode: '521', logo: 'x', status: true } },
  { method: 'put', path: `/api/v1/caccounting/bank/init_update_image/${UUID_ZERO}`, domain: 'banque', multipart: true },
  { method: 'put', path: `/api/v1/caccounting/bank/validate_or_reject/${UUID_ZERO}`, domain: 'banque', sampleBody: { decision: 'ACCEPTED', notes: 'x', checkerOperationType: 'CREATE' } },

  // --- Référentiel : comptes ------------------------------------------------
  { method: 'post', path: '/api/v1/caccounting/compte/init_create', domain: 'compte', sampleBody: { code: '999999', intitule: 'x', niveau: 1 } },
  { method: 'get', path: '/api/v1/caccounting/compte', domain: 'compte' },
  { method: 'get', path: '/api/v1/caccounting/compte/list', domain: 'compte' },
  { method: 'get', path: `/api/v1/caccounting/compte/${UUID_ZERO}`, domain: 'compte' },
  { method: 'get', path: `/api/v1/caccounting/compte/active_by_id/${UUID_ZERO}`, domain: 'compte' },
  { method: 'put', path: `/api/v1/caccounting/compte/init_update/${UUID_ZERO}`, domain: 'compte', sampleBody: { code: '999999', intitule: 'x', niveau: 1 } },
  { method: 'delete', path: `/api/v1/caccounting/compte/${UUID_ZERO}`, domain: 'compte', destructive: true },
  { method: 'put', path: `/api/v1/caccounting/compte/validate_or_reject/${UUID_ZERO}`, domain: 'compte', sampleBody: { decision: 'ACCEPTED', notes: 'x', checkerOperationType: 'CREATE' } },

  // --- Référentiel : tiers --------------------------------------------------
  { method: 'post', path: '/api/v1/caccounting/tiers/init_create', domain: 'tiers', sampleBody: { code: 'X', raisonSociale: 'x', typeTiers: 'CLIENT', compteCollectifCode: '411' } },
  { method: 'get', path: '/api/v1/caccounting/tiers', domain: 'tiers' },
  { method: 'get', path: '/api/v1/caccounting/tiers/list', domain: 'tiers' },
  { method: 'get', path: `/api/v1/caccounting/tiers/${UUID_ZERO}`, domain: 'tiers' },
  { method: 'get', path: `/api/v1/caccounting/tiers/active_by_id/${UUID_ZERO}`, domain: 'tiers' },
  { method: 'put', path: `/api/v1/caccounting/tiers/init_update/${UUID_ZERO}`, domain: 'tiers', sampleBody: { code: 'X', raisonSociale: 'x', typeTiers: 'CLIENT', compteCollectifCode: '411', actif: true } },
  { method: 'put', path: `/api/v1/caccounting/tiers/validate_or_reject/${UUID_ZERO}`, domain: 'tiers', sampleBody: { decision: 'ACCEPTED', notes: 'x', checkerOperationType: 'CREATE' } },

  // --- Référentiel : journaux -----------------------------------------------
  { method: 'post', path: '/api/comptabilite/journaux/init_create', domain: 'journal', sampleBody: { code: 'OD', intitule: 'x', typeJournal: 'DIVERS' } },
  { method: 'get', path: '/api/comptabilite/journaux', domain: 'journal' },
  { method: 'get', path: '/api/comptabilite/journaux/list', domain: 'journal' },
  { method: 'get', path: `/api/comptabilite/journaux/${UUID_ZERO}`, domain: 'journal' },
  { method: 'get', path: `/api/comptabilite/journaux/active_by_id/${UUID_ZERO}`, domain: 'journal' },
  { method: 'put', path: `/api/comptabilite/journaux/init_update/${UUID_ZERO}`, domain: 'journal', sampleBody: { code: 'OD', intitule: 'x', typeJournal: 'DIVERS', actif: true } },
  { method: 'put', path: `/api/comptabilite/journaux/${UUID_ZERO}/toggle`, domain: 'journal', destructive: true },
  { method: 'put', path: `/api/comptabilite/journaux/validate_or_reject/${UUID_ZERO}`, domain: 'journal', sampleBody: { decision: 'ACCEPTED', notes: 'x', checkerOperationType: 'CREATE' } },

  // --- Écritures ------------------------------------------------------------
  { method: 'post', path: '/api/comptabilite/ecritures', domain: 'ecriture', sampleBody: { journalId: UUID_ZERO, numeroPiece: 'x', libelle: 'x', dateComptable: DATE, lignes: [{ compteCode: '601', debit: 1 }] } },
  { method: 'post', path: '/api/comptabilite/ecritures/simuler-tva', domain: 'ecriture', sampleBody: { montantHt: 100, tauxTva: 19.25, compteHtCode: '601' } },
  { method: 'post', path: `/api/comptabilite/ecritures/${UUID_ZERO}/soumettre`, domain: 'ecriture' },
  { method: 'post', path: `/api/comptabilite/ecritures/${UUID_ZERO}/valider`, domain: 'ecriture' },
  { method: 'post', path: `/api/comptabilite/ecritures/${UUID_ZERO}/rejeter`, domain: 'ecriture' },
  { method: 'get', path: `/api/comptabilite/ecritures/${UUID_ZERO}`, domain: 'ecriture' },

  // --- Immobilisations ------------------------------------------------------
  { method: 'get', path: '/api/v1/immobilisations', domain: 'immobilisation' },
  { method: 'get', path: '/api/v1/immobilisations/pending', domain: 'immobilisation' },
  { method: 'get', path: `/api/v1/immobilisations/${UUID_ZERO}`, domain: 'immobilisation' },
  { method: 'get', path: `/api/v1/immobilisations/${UUID_ZERO}/plan`, domain: 'immobilisation' },
  { method: 'post', path: '/api/v1/immobilisations/init_create', domain: 'immobilisation', sampleBody: { code: 'X', designation: 'x', valeurOrigine: 1, dateAcquisition: DATE, dateMiseEnService: DATE, modeAmortissement: 'LINEAIRE', dureeUtile: 5 } },
  { method: 'put', path: `/api/v1/immobilisations/init_update/${UUID_ZERO}`, domain: 'immobilisation', sampleBody: { code: 'X', designation: 'x', valeurOrigine: 1, dateAcquisition: DATE, dateMiseEnService: DATE, modeAmortissement: 'LINEAIRE', dureeUtile: 5, statut: 'ACTIVE' } },
  { method: 'post', path: '/api/v1/immobilisations/init_amortir', domain: 'immobilisation', sampleBody: { annee: 2025, compteImmoCode: '241' } },
  { method: 'put', path: `/api/v1/immobilisations/validate_or_reject/${UUID_ZERO}`, domain: 'immobilisation', sampleBody: { decision: 'ACCEPTED', notes: 'x', checkerOperationType: 'CREATE' } },

  // --- Clôture --------------------------------------------------------------
  { method: 'post', path: '/api/comptabilite/cloture/2025', domain: 'cloture', destructive: true },
  { method: 'post', path: '/api/comptabilite/cloture/reevaluer', domain: 'cloture', destructive: true, sampleBody: { annee: 2025, coursCloture: { EUR: 655.957 } } },

  // --- Rapprochement bancaire (comptabilité) --------------------------------
  { method: 'get', path: '/api/comptabilite/rapprochement/releves', domain: 'rapprochement' },
  { method: 'get', path: `/api/comptabilite/rapprochement/releves/${UUID_ZERO}`, domain: 'rapprochement' },
  { method: 'post', path: '/api/comptabilite/rapprochement/manuel', domain: 'rapprochement', sampleBody: { banqueId: UUID_ZERO, dateReleve: DATE, soldeInitial: 0, soldeFinal: 0, lignes: [{ dateTransaction: DATE, libelle: 'x', montant: 1 }] } },
  { method: 'post', path: '/api/comptabilite/rapprochement/synchroniser', domain: 'rapprochement', sampleBody: { banqueId: UUID_ZERO, dateReleve: DATE, soldeInitial: 0 } },
  { method: 'post', path: `/api/comptabilite/rapprochement/${UUID_ZERO}/rapprocher`, domain: 'rapprochement', sampleParams: { compteBanqueCode: '521' } },

  // --- Reporting comptable --------------------------------------------------
  { method: 'get', path: '/api/comptabilite/reporting/livre-journal', domain: 'reporting', sampleParams: { debut: DATE, fin: DATE } },
  { method: 'get', path: '/api/comptabilite/reporting/grand-livre', domain: 'reporting', sampleParams: { debut: DATE, fin: DATE } },
  { method: 'get', path: '/api/comptabilite/reporting/balance', domain: 'reporting', sampleParams: { debut: DATE, fin: DATE } },
  { method: 'get', path: '/api/comptabilite/reporting/bilan', domain: 'reporting', sampleParams: { dateBilan: DATE } },
  { method: 'get', path: '/api/comptabilite/reporting/compte-resultat', domain: 'reporting', sampleParams: { annee: 2025 } },
  { method: 'get', path: '/api/comptabilite/reporting/tft', domain: 'reporting', sampleParams: { annee: 2025 } },
  { method: 'get', path: '/api/comptabilite/reporting/tva', domain: 'reporting', sampleParams: { annee: 2025, mois: 1 } },
  { method: 'get', path: '/api/comptabilite/reporting/fec', domain: 'reporting', sampleParams: { annee: 2025 } },

  // --- Utilisateurs ---------------------------------------------------------
  { method: 'post', path: '/api/v1/users/init_create', domain: 'users', multipart: true },
  { method: 'get', path: '/api/v1/users', domain: 'users' },
  { method: 'get', path: '/api/v1/users/pending', domain: 'users' },
  { method: 'get', path: '/api/v1/users/search', domain: 'users' },
  { method: 'get', path: `/api/v1/users/${UUID_ZERO}`, domain: 'users' },
  { method: 'put', path: `/api/v1/users/init_update/${UUID_ZERO}`, domain: 'users', sampleBody: { nom: 'x', prenom: 'x', email: 'x@y.z', telephones: ['690000000'], actif: true } },
  { method: 'put', path: `/api/v1/users/init_change_photo/${UUID_ZERO}`, domain: 'users', multipart: true },
  { method: 'put', path: `/api/v1/users/init_update_permissions/${UUID_ZERO}`, domain: 'users', sampleBody: { permissions: ['GET_FULL_USER_INFO'] } },
  { method: 'put', path: `/api/v1/users/validate_or_reject/${UUID_ZERO}`, domain: 'users', sampleBody: { decision: 'ACCEPTED', notes: 'x', checkerOperationType: 'CREATE' } },
];

/** Routes nécessitant une authentification. */
export const protectedEndpoints = endpoints.filter((e) => !e.public);

/** Routes en lecture seule, sûres à appeler en boucle. */
export const readOnlyEndpoints = endpoints.filter((e) => e.method === 'get' && !e.destructive);
