import { APIRequestContext, APIResponse } from '@playwright/test';
import { BaseApiClient, cleanParams } from './BaseApiClient';
import { PageQuery, PageRecord } from '../models/common';
import {
  EcheanceOutput,
  FinancementOutput,
  FinancementSmartOutput,
  SimulationQuery,
} from '../models/financement';

/** Corps de création d'un financement (CreerFinancementRequest). */
export interface CreerFinancementBody {
  banqueId?: string;
  intitule?: string;
  type?: string;
  capital?: number;
  tauxNominal?: number;
  dateEffet?: string;
  dureeMois?: number;
  periodicite?: string;
  utilisateurId?: string;
}

/** Corps de création d'un engagement hors-bilan (CreerHorsBilanRequest). */
export interface CreerHorsBilanBody {
  type?: string;
  intitule?: string;
  tiersId?: string;
  montant?: number;
  dateEffet?: string;
  dateEcheance?: string;
}

/** Client des endpoints /api/financement. */
export class FinancementApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/financement');
  }

  /** Recherche paginée, filtrable par prêteur et par nature. */
  async lister(
    query: PageQuery & { banqueId?: string; type?: string } = {},
  ): Promise<PageRecord<FinancementSmartOutput>> {
    const response = await this.get('', { params: cleanParams(query) });
    return this.json<PageRecord<FinancementSmartOutput>>(response);
  }

  /** Consultation unitaire avec échéancier. */
  async getById(id: string): Promise<FinancementOutput> {
    const response = await this.get(`/${id}`);
    return this.json<FinancementOutput>(response);
  }

  /** Consultation unitaire sans assertion de statut (cas 404 / id invalide). */
  async getByIdRaw(id: string, expectStatus: number[]): Promise<APIResponse> {
    return this.get(`/${id}`, { expectStatus });
  }

  /** Enregistrement d'un financement, plan d'amortissement inclus. */
  async creer(body: CreerFinancementBody): Promise<FinancementOutput> {
    const response = await this.post('', { data: body });
    return this.json<FinancementOutput>(response);
  }

  /** Simulation d'un plan d'amortissement, sans persistance. */
  async simuler(query: SimulationQuery): Promise<EcheanceOutput[]> {
    const response = await this.get('/simuler', { params: cleanParams(query) });
    return this.json<EcheanceOutput[]>(response);
  }

  /** Règlement d'une échéance. */
  async payerEcheance(echeanceId: string, userId: string, expectStatus?: number[]) {
    return this.post(`/echeances/${echeanceId}/payer`, {
      params: { userId },
      expectStatus,
    });
  }

  async creerHorsBilan(body: CreerHorsBilanBody): Promise<unknown> {
    const response = await this.post('/hors-bilan', { data: body });
    return this.json<unknown>(response);
  }

  async reportingHorsBilan(): Promise<unknown[]> {
    const response = await this.get('/reporting/hors-bilan');
    return this.json<unknown[]>(response);
  }

  async kpis(): Promise<Record<string, unknown>> {
    const response = await this.get('/reporting/kpis');
    return this.json<Record<string, unknown>>(response);
  }
}
