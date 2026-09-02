import { APIRequestContext } from '@playwright/test';
import { BaseApiClient } from './BaseApiClient';

/** Client des endpoints /api/comptabilite/analytique (axes, sections, ventilations). */
export class AnalytiqueApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/analytique');
  }

  async creerAxe(body: { code?: string; intitule?: string }, expectStatus?: number[]) {
    return this.post('/axes', { data: body, expectStatus });
  }

  async listerAxes(): Promise<Record<string, unknown>[]> {
    const response = await this.get('/axes');
    return this.json<Record<string, unknown>[]>(response);
  }

  async modifierStatutAxe(id: string, actif: boolean, expectStatus?: number[]) {
    return this.put(`/axes/${id}/statut`, { params: { actif }, expectStatus });
  }

  async creerSection(
    axeId: string,
    body: { code?: string; intitule?: string },
    expectStatus?: number[],
  ) {
    return this.post(`/axes/${axeId}/sections`, { data: body, expectStatus });
  }

  async listerSections(axeId: string, expectStatus?: number[]) {
    return this.get(`/axes/${axeId}/sections`, { expectStatus });
  }

  async modifierStatutSection(id: string, actif: boolean, expectStatus?: number[]) {
    return this.put(`/sections/${id}/statut`, { params: { actif }, expectStatus });
  }

  async ventilerLigne(
    ligneId: string,
    ventilations: { sectionId?: string; pourcentage?: number }[],
    expectStatus?: number[],
  ) {
    return this.post(`/lignes/${ligneId}/ventiler`, { data: ventilations, expectStatus });
  }
}

/** Client des endpoints /api/comptabilite/analytique/budgets. */
export class AnalytiqueBudgetApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/analytique/budgets');
  }

  async definirBudget(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('', { data: body, expectStatus });
  }

  async listerParAnnee(annee: number): Promise<Record<string, unknown>[]> {
    const response = await this.get(`/${annee}`);
    return this.json<Record<string, unknown>[]>(response);
  }

  async listerParSection(annee: number, sectionId: string, expectStatus?: number[]) {
    return this.get(`/${annee}/sections/${sectionId}`, { expectStatus });
  }
}

/** Client des endpoints /api/comptabilite/analytique/cles. */
export class CleRepartitionApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/analytique/cles');
  }

  async creerCle(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('', { data: body, expectStatus });
  }

  async listerCles(): Promise<Record<string, unknown>[]> {
    const response = await this.get('');
    return this.json<Record<string, unknown>[]>(response);
  }

  async appliquerCle(ligneId: string, cleId: string, expectStatus?: number[]) {
    return this.post(`/lignes/${ligneId}/appliquer/${cleId}`, { expectStatus });
  }
}

/** Client des endpoints /api/comptabilite/analytique/reporting. */
export class ReportingAnalytiqueApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/analytique/reporting');
  }

  async grandLivre(debut: string, fin: string): Promise<Record<string, unknown>[]> {
    const response = await this.get('/grand-livre', { params: { debut, fin } });
    return this.json<Record<string, unknown>[]>(response);
  }

  async balance(debut: string, fin: string): Promise<Record<string, unknown>[]> {
    const response = await this.get('/balance', { params: { debut, fin } });
    return this.json<Record<string, unknown>[]>(response);
  }

  async compteResultat(sectionId: string, annee: number, expectStatus?: number[]) {
    return this.get(`/sections/${sectionId}/resultat/${annee}`, { expectStatus });
  }

  async suiviBudgetaire(sectionId: string, annee: number, expectStatus?: number[]) {
    return this.get(`/sections/${sectionId}/suivi-budgetaire/${annee}`, { expectStatus });
  }
}
