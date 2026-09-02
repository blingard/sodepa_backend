import { APIRequestContext } from '@playwright/test';
import { BaseApiClient, cleanParams } from './BaseApiClient';

/** Client des endpoints /api/tresorerie. */
export class TresorerieApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/tresorerie');
  }

  async listerPrevisions(debut: string, fin: string): Promise<Record<string, unknown>[]> {
    const response = await this.get('/previsions', { params: { debut, fin } });
    return this.json<Record<string, unknown>[]>(response);
  }

  async ajouterPrevision(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/previsions', { data: body, expectStatus });
  }

  async cashFlow(debut: string, fin: string): Promise<Record<string, unknown>[]> {
    const response = await this.get('/cash-flow', { params: { debut, fin } });
    return this.json<Record<string, unknown>[]>(response);
  }

  async bfr(date: string): Promise<Record<string, unknown>> {
    const response = await this.get('/bfr', { params: { date } });
    return this.json<Record<string, unknown>>(response);
  }

  async alertesDecouvert(): Promise<Record<string, unknown>[]> {
    const response = await this.get('/decouverts/alertes');
    return this.json<Record<string, unknown>[]>(response);
  }

  async whatIf(
    croissance: number,
    inflation: number,
    prixRevient: number,
  ): Promise<Record<string, unknown>> {
    const response = await this.get('/simulations/what-if', {
      params: { croissance, inflation, prixRevient },
    });
    return this.json<Record<string, unknown>>(response);
  }
}

/** Client des endpoints /api/tresorerie/change (couverture de change). */
export class ChangeHedgingApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/tresorerie/change');
  }

  async listerCouvertures(
    query: { devise?: string; statut?: string } = {},
  ): Promise<Record<string, unknown>[]> {
    const response = await this.get('/couverture', { params: cleanParams(query) });
    return this.json<Record<string, unknown>[]>(response);
  }

  async enregistrerCouverture(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/couverture', { data: body, expectStatus });
  }

  async evaluer(id: string, coursSpot: number, expectStatus?: number[]) {
    return this.get(`/couverture/${id}/evaluer`, { params: { coursSpot }, expectStatus });
  }
}

/** Client des endpoints /api/tresorerie/rapprochement (matching et arbitrage). */
export class RapprochementBancaireApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/tresorerie/rapprochement');
  }

  async matcher(releveId: string, expectStatus?: number[]) {
    return this.post('/matching', { params: { releveId }, expectStatus });
  }

  async arbitrage(
    fondsSecurite: number,
    debut: string,
    fin: string,
    soldeActuel: number,
  ): Promise<Record<string, unknown>[]> {
    const response = await this.get('/arbitrage', {
      params: { fondsSecurite, debut, fin, soldeActuel },
    });
    return this.json<Record<string, unknown>[]>(response);
  }
}

/** Client des endpoints /api/reporting (pilotage stratégique). */
export class PilotageApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/reporting');
  }

  async tft(annee: number): Promise<Record<string, unknown>> {
    const response = await this.get('/tft', { params: { annee } });
    return this.json<Record<string, unknown>>(response);
  }

  async runway(): Promise<Record<string, unknown>> {
    const response = await this.get('/runway');
    return this.json<Record<string, unknown>>(response);
  }
}
