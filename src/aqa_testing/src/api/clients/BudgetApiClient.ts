import { APIRequestContext } from '@playwright/test';
import { BaseApiClient, cleanParams } from './BaseApiClient';
import { PageQuery, PageRecord } from '../models/common';
import { BudgetItemOutput, BudgetPlanOutput, EngagementOutput } from '../models/budget';

/** Corps de création d'un plan budgétaire (CreerBudgetRequest). */
export interface CreerBudgetBody {
  annee?: number;
  intitule?: string;
  utilisateurId?: string;
}

/** Corps d'ajout d'un poste budgétaire (AjouterItemRequest). */
export interface AjouterItemBody {
  compteCode?: string;
  sectionId?: string;
  montant?: number;
}

/** Corps d'une réallocation (ReallocationRequest). */
export interface ReallocationBody {
  sourceItemId?: string;
  destItemId?: string;
  montant?: number;
  responsableId?: string;
  raison?: string;
}

/** Corps d'un engagement de dépense (EngagementRequest). */
export interface EngagementBody {
  planId?: string;
  compteCode?: string;
  sectionId?: string;
  numeroEngagement?: string;
  description?: string;
  montant?: number;
  utilisateurId?: string;
}

/** Client des endpoints /api/budget (plans et engagements). */
export class BudgetApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/budget');
  }

  async listerPlans(
    query: PageQuery & { annee?: number; statut?: string } = {},
  ): Promise<PageRecord<BudgetPlanOutput>> {
    const response = await this.get('/plans', { params: cleanParams(query) });
    return this.json<PageRecord<BudgetPlanOutput>>(response);
  }

  async getPlan(planId: string): Promise<BudgetPlanOutput> {
    const response = await this.get(`/plans/${planId}`);
    return this.json<BudgetPlanOutput>(response);
  }

  async listerEngagements(
    query: PageQuery & { planId?: string; statut?: string } = {},
  ): Promise<PageRecord<EngagementOutput>> {
    const response = await this.get('/engagements', { params: cleanParams(query) });
    return this.json<PageRecord<EngagementOutput>>(response);
  }

  async getEngagement(numero: string): Promise<EngagementOutput> {
    const response = await this.get(`/engagements/${encodeURIComponent(numero)}`);
    return this.json<EngagementOutput>(response);
  }

  async creerPlan(body: CreerBudgetBody): Promise<BudgetPlanOutput> {
    const response = await this.post('/plans', { data: body });
    return this.json<BudgetPlanOutput>(response);
  }

  async ajouterItem(planId: string, body: AjouterItemBody): Promise<BudgetItemOutput> {
    const response = await this.post(`/plans/${planId}/items`, { data: body });
    return this.json<BudgetItemOutput>(response);
  }

  async soumettrePlan(planId: string, userId: string, expectStatus?: number[]) {
    return this.post(`/plans/${planId}/soumettre`, { params: { userId }, expectStatus });
  }

  async approuverPlan(planId: string, userId: string, expectStatus?: number[]) {
    return this.post(`/plans/${planId}/approuver`, { params: { userId }, expectStatus });
  }

  async rejeterPlan(planId: string, userId: string, expectStatus?: number[]) {
    return this.post(`/plans/${planId}/rejeter`, { params: { userId }, expectStatus });
  }

  async reallocer(body: ReallocationBody, expectStatus?: number[]) {
    return this.post('/reallocations', { data: body, expectStatus });
  }

  async engager(body: EngagementBody): Promise<EngagementOutput> {
    const response = await this.post('/engagements', { data: body });
    return this.json<EngagementOutput>(response);
  }

  async liquider(numero: string, userId: string, expectStatus?: number[]) {
    return this.post(`/engagements/${encodeURIComponent(numero)}/liquider`, {
      params: { userId },
      expectStatus,
    });
  }

  async annuler(numero: string, userId: string, expectStatus?: number[]) {
    return this.post(`/engagements/${encodeURIComponent(numero)}/annuler`, {
      params: { userId },
      expectStatus,
    });
  }
}

/** Client des endpoints /api/budget/collaboratif. */
export class BudgetCollaboratifApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/budget/collaboratif');
  }

  async listerDemandes(
    query: { departementId?: string; annee?: number; statut?: string } = {},
  ): Promise<unknown[]> {
    const response = await this.get('/demandes', { params: cleanParams(query) });
    return this.json<unknown[]>(response);
  }

  async saisirDemande(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/demandes', { data: body, expectStatus });
  }

  async soumettreDemandes(departementId: string, annee: number, expectStatus?: number[]) {
    return this.post('/demandes/soumettre', { params: { departementId, annee }, expectStatus });
  }

  async approuverDemande(demandeId: string, userId: string, expectStatus?: number[]) {
    return this.post(`/demandes/${demandeId}/approuver`, { params: { userId }, expectStatus });
  }

  async rejeterDemande(
    demandeId: string,
    motif: string,
    userId: string,
    expectStatus?: number[],
  ) {
    return this.post(`/demandes/${demandeId}/rejeter`, { params: { motif, userId }, expectStatus });
  }

  async appliquerCadrage(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/cadrage', { data: body, expectStatus });
  }

  async genererDepuisHistorique(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/generer', { data: body, expectStatus });
  }

  async consolider(annee: number, planId: string, userId: string, expectStatus?: number[]) {
    return this.post('/consolider', { params: { annee, planId, userId }, expectStatus });
  }
}

/** Client des endpoints /api/budget/engagements/workflow. */
export class EngagementWorkflowApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/budget/engagements/workflow');
  }

  async preEngager(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/pre-engager', { data: body, expectStatus });
  }

  async validerEtape(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/valider', { data: body, expectStatus });
  }

  async rejeter(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/rejeter', { data: body, expectStatus });
  }
}
