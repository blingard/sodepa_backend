import { APIRequestContext } from '@playwright/test';
import { BaseApiClient, cleanParams } from './BaseApiClient';
import { PageQuery, PageRecord } from '../models/common';

/** Décision maker-checker, commune à tous les référentiels. */
export interface DecisionBody {
  decision?: 'PENDING' | 'REJECTED' | 'EXPIRED' | 'ACCEPTED';
  notes?: string;
  checkerOperationType?: 'CREATE' | 'UPDATE' | 'UPDATE_IMAGE';
}

/** Client des endpoints /api/v1/caccounting/bank. */
export class BanqueApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/v1/caccounting/bank');
  }

  async page(query: PageQuery = {}): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async list(): Promise<Record<string, unknown>[]> {
    const response = await this.get('/list');
    return this.json<Record<string, unknown>[]>(response);
  }

  async getById(id: string, expectStatus?: number[]) {
    return this.get(`/${id}`, { expectStatus });
  }

  async getActiveById(id: string, expectStatus?: number[]) {
    return this.get(`/active_by_id/${id}`, { expectStatus });
  }

  async initUpdate(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/init_update/${id}`, { data: body, expectStatus });
  }

  async validateOrReject(id: string, body: DecisionBody, expectStatus?: number[]) {
    return this.put(`/validate_or_reject/${id}`, { data: body, expectStatus });
  }
}

/** Client des endpoints /api/v1/caccounting/compte. */
export class CompteApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/v1/caccounting/compte');
  }

  async initCreate(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/init_create', { data: body, expectStatus });
  }

  async page(query: PageQuery = {}): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async list(): Promise<Record<string, unknown>[]> {
    const response = await this.get('/list');
    return this.json<Record<string, unknown>[]>(response);
  }

  async getById(id: string, expectStatus?: number[]) {
    return this.get(`/${id}`, { expectStatus });
  }

  async getActiveById(id: string, expectStatus?: number[]) {
    return this.get(`/active_by_id/${id}`, { expectStatus });
  }

  async initUpdate(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/init_update/${id}`, { data: body, expectStatus });
  }

  async supprimer(id: string, expectStatus?: number[]) {
    return this.delete(`/${id}`, { expectStatus });
  }

  async validateOrReject(id: string, body: DecisionBody, expectStatus?: number[]) {
    return this.put(`/validate_or_reject/${id}`, { data: body, expectStatus });
  }
}

/** Client des endpoints /api/v1/caccounting/tiers. */
export class TiersApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/v1/caccounting/tiers');
  }

  async initCreate(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/init_create', { data: body, expectStatus });
  }

  async page(query: PageQuery = {}): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async list(): Promise<Record<string, unknown>[]> {
    const response = await this.get('/list');
    return this.json<Record<string, unknown>[]>(response);
  }

  async getById(id: string, expectStatus?: number[]) {
    return this.get(`/${id}`, { expectStatus });
  }

  async getActiveById(id: string, expectStatus?: number[]) {
    return this.get(`/active_by_id/${id}`, { expectStatus });
  }

  async initUpdate(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/init_update/${id}`, { data: body, expectStatus });
  }

  async validateOrReject(id: string, body: DecisionBody, expectStatus?: number[]) {
    return this.put(`/validate_or_reject/${id}`, { data: body, expectStatus });
  }
}

/** Client des endpoints /api/comptabilite/journaux. */
export class JournalApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/comptabilite/journaux');
  }

  async initCreate(body: Record<string, unknown>, expectStatus?: number[]) {
    return this.post('/init_create', { data: body, expectStatus });
  }

  async page(query: PageQuery = {}): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async list(): Promise<Record<string, unknown>[]> {
    const response = await this.get('/list');
    return this.json<Record<string, unknown>[]>(response);
  }

  async getById(id: string, expectStatus?: number[]) {
    return this.get(`/${id}`, { expectStatus });
  }

  async getActiveById(id: string, expectStatus?: number[]) {
    return this.get(`/active_by_id/${id}`, { expectStatus });
  }

  async initUpdate(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/init_update/${id}`, { data: body, expectStatus });
  }

  async toggle(id: string, expectStatus?: number[]) {
    return this.put(`/${id}/toggle`, { expectStatus });
  }

  async validateOrReject(id: string, body: DecisionBody, expectStatus?: number[]) {
    return this.put(`/validate_or_reject/${id}`, { data: body, expectStatus });
  }
}

/** Client des endpoints /api/v1/users. */
export class UserApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/v1/users');
  }

  async page(query: PageQuery = {}): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async pending(query: PageQuery = {}): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('/pending', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async search(
    query: PageQuery & { nom?: string; prenom?: string; email?: string; telephone?: string } = {},
  ): Promise<PageRecord<Record<string, unknown>>> {
    const response = await this.get('/search', { params: cleanParams(query) });
    return this.json<PageRecord<Record<string, unknown>>>(response);
  }

  async getById(id: string, expectStatus?: number[]) {
    return this.get(`/${id}`, { expectStatus });
  }

  async initUpdate(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/init_update/${id}`, { data: body, expectStatus });
  }

  async initUpdatePermissions(id: string, body: Record<string, unknown>, expectStatus?: number[]) {
    return this.put(`/init_update_permissions/${id}`, { data: body, expectStatus });
  }

  async validateOrReject(id: string, body: DecisionBody, expectStatus?: number[]) {
    return this.put(`/validate_or_reject/${id}`, { data: body, expectStatus });
  }
}
