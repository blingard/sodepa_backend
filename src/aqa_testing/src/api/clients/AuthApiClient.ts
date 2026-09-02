import { APIRequestContext, APIResponse } from '@playwright/test';
import { BaseApiClient } from './BaseApiClient';
import { TokenResponse } from '../models/common';

/** Client des endpoints /api/auth. */
export class AuthApiClient extends BaseApiClient {
  constructor(request: APIRequestContext) {
    super(request, '/api/auth');
  }

  /** Connexion : renvoie la réponse brute (utile pour tester les cas d'échec). */
  async loginRaw(username: string, password: string): Promise<APIResponse> {
    return this.post('/login', {
      data: { username, password },
      expectStatus: [200, 400, 401, 403, 500],
    });
  }

  /** Connexion nominale : échoue le test si le backend ne renvoie pas 200. */
  async login(username: string, password: string): Promise<TokenResponse> {
    const response = await this.post('/login', { data: { username, password } });
    return this.json<TokenResponse>(response);
  }

  async refresh(refreshToken: string): Promise<TokenResponse> {
    const response = await this.post('/refresh', { data: { refreshToken } });
    return this.json<TokenResponse>(response);
  }

  async logout(refreshToken: string): Promise<void> {
    await this.post('/logout', { data: { refreshToken } });
  }

  async listSessions(): Promise<unknown[]> {
    const response = await this.get('/sessions');
    return this.json<unknown[]>(response);
  }
}
