import { APIRequestContext } from '@playwright/test';
import { BaseApiClient, cleanParams } from './BaseApiClient';
import { PageQuery, PageRecord } from '../models/common';

/** Client des endpoints /api/comptabilite/ecritures. */
export class EcritureApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/ecritures');
  }

  async saisir(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('', { data: body, expectStatus });
  }

  async simulerTva(
    body: { montantHt?: number; tauxTva?: number; compteHtCode?: string },
    expectStatus?: number[],
  ) {
    return this.post('/simuler-tva', { data: body, expectStatus });
  }

  async soumettre(id: string, expectStatus?: number[]) {
    return this.post(`/${id}/soumettre`, { expectStatus });
  }

  async valider(id: string, expectStatus?: number[]) {
    return this.post(`/${id}/valider`, { expectStatus });
  }

  async rejeter(id: string, expectStatus?: number[]) {
    return this.post(`/${id}/rejeter`, { expectStatus });
  }

  async getById(id: string, expectStatus?: number[]) {
    return this.get(`/${id}`, { expectStatus });
  }
}

/** Client des endpoints /api/v1/immobilisations. */
export class ImmobilisationApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/v1/immobilisations');
  }

  async page(
    query: PageQuery & { recherche?: string; statut?: string } = {},
  ): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async pending(query: PageQuery = {}): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('/pending', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async getById(id: string, expectStatus?: number[]) {
    return this.get(`/${id}`, { expectStatus });
  }

  async planAmortissement(id: string, expectStatus?: number[]) {
    return this.get(`/${id}/plan`, { expectStatus });
  }

  async initCreate(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/init_create', { data: body, expectStatus });
  }

  async initUpdate(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/init_update/${id}`, { data: body, expectStatus });
  }

  async initAmortir(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/init_amortir', { data: body, expectStatus });
  }

  async validateOrReject(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/validate_or_reject/${id}`, { data: body, expectStatus });
  }
}

/** Client des endpoints /api/comptabilite/cloture. */
export class ClotureApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/cloture');
  }

  async cloturerExercice(annee: number, expectStatus?: number[]) {
    return this.post(`/${annee}`, { expectStatus });
  }

  async reevaluerDevises(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/reevaluer', { data: body, expectStatus });
  }
}

/** Client des endpoints /api/comptabilite/rapprochement (relevés bancaires). */
export class RapprochementApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/rapprochement');
  }

  async listerReleves(
    query: PageQuery & { banqueId?: string; valide?: boolean } = {},
  ): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('/releves', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async getReleve(releveId: string, expectStatus?: number[]) {
    return this.get(`/releves/${releveId}`, { expectStatus });
  }

  async saisirReleveManuel(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/manuel', { data: body, expectStatus });
  }

  async synchroniser(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/synchroniser', { data: body, expectStatus });
  }

  async rapprocher(releveId: string, compteBanqueCode: string, expectStatus?: number[]) {
    return this.post(`/${releveId}/rapprocher`, { params: { compteBanqueCode }, expectStatus });
  }
}

/** Client des endpoints /api/comptabilite/reporting (états OHADA). */
export class ReportingApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/reporting');
  }

  async livreJournal(debut: string, fin: string): Promise<Record<string, unknown>[]> {
    const response = await this.get('/livre-journal', { params: { debut, fin } });
    return this.json<Record<string, unknown>[]>(response);
  }

  async grandLivre(debut: string, fin: string): Promise<Record<string, unknown>[]> {
    const response = await this.get('/grand-livre', { params: { debut, fin } });
    return this.json<Record<string, unknown>[]>(response);
  }

  async balance(debut: string, fin: string): Promise<Record<string, unknown>[]> {
    const response = await this.get('/balance', { params: { debut, fin } });
    return this.json<Record<string, unknown>[]>(response);
  }

  async bilan(dateBilan: string): Promise<Record<string, unknown>> {
    const response = await this.get('/bilan', { params: { dateBilan } });
    return this.json<Record<string, unknown>>(response);
  }

  async compteResultat(annee: number): Promise<Record<string, unknown>> {
    const response = await this.get('/compte-resultat', { params: { annee } });
    return this.json<Record<string, unknown>>(response);
  }

  async tft(annee: number): Promise<Record<string, unknown>> {
    const response = await this.get('/tft', { params: { annee } });
    return this.json<Record<string, unknown>>(response);
  }

  async tva(annee: number, mois: number): Promise<Record<string, unknown>> {
    const response = await this.get('/tva', { params: { annee, mois } });
    return this.json<Record<string, unknown>>(response);
  }

  async fec(annee: number): Promise<string> {
    const response = await this.get('/fec', { params: { annee } });
    return response.text();
  }
}

/** Client des endpoints d'audit : /api/auth/audit et /api/audit. */
export class AuditApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/auth/audit');
  }

  async mesActivites(expectStatus?: number[]) {
    return this.get('/activities', { expectStatus });
  }

  async transactionsClickHouse(limit?: number, expectStatus?: number[]) {
    return this.get('/clickhouse/transactions', {
      params: limit === undefined ? undefined : { limit },
      expectStatus,
    });
  }

  async activitesClickHouse(limit?: number, expectStatus?: number[]) {
    return this.get('/clickhouse/activities', {
      params: limit === undefined ? undefined : { limit },
      expectStatus,
    });
  }

  async requeteAnalytique(query: string, expectStatus?: number[]) {
    return this.get('/analytics', { params: { query }, expectStatus });
  }
}

/** Client de la piste d'audit métier : /api/audit/logs. */
export class AuditTrailApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/audit');
  }

  async logs(entiteNom: string, entiteId: string, expectStatus?: number[]) {
    return this.get('/logs', { params: { entiteNom, entiteId }, expectStatus });
  }
}
