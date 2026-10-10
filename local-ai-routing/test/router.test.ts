import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import http from 'http';
import { createRouterApp } from '../src/router';
import { AppConfig } from '../src/config';

describe('Router Server Express App', () => {
  let server: http.Server;
  let baseUrl: string;

  const testConfig: AppConfig = {
    port: 0,
    ollamaHost: 'http://127.0.0.1:11434',
    modelName: 'gemma3:4b',
    maxBodySize: '1mb',
    requestTimeoutMs: 5000,
    pairingToken: '',
  };

  beforeAll(async () => {
    const app = createRouterApp(testConfig);
    await new Promise<void>((resolve) => {
      server = app.listen(0, '127.0.0.1', () => {
        const addr: any = server.address();
        baseUrl = `http://127.0.0.1:${addr.port}`;
        resolve();
      });
    });
  });

  afterAll(async () => {
    await new Promise<void>((resolve) => server.close(() => resolve()));
  });

  it('GET /health returns health status object', async () => {
    const res = await fetch(`${baseUrl}/health`);
    expect(res.status).toBe(200);
    const json: any = await res.json();
    expect(json).toHaveProperty('router', 'online');
    expect(json).toHaveProperty('ollamaHost', 'http://127.0.0.1:11434');
    expect(json).toHaveProperty('targetModel', 'gemma3:4b');
  });

  it('GET /api/unknown returns 404', async () => {
    const res = await fetch(`${baseUrl}/api/unknown`);
    expect(res.status).toBe(404);
    const json: any = await res.json();
    expect(json.error).toContain('not supported');
  });

  it('POST /api/chat rejects missing models with 404 when model not present', async () => {
    const res = await fetch(`${baseUrl}/api/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ model: 'nonexistent-model-xyz', messages: [] }),
    });
    expect([404, 503]).toContain(res.status);
    const json: any = await res.json();
    expect(json).toHaveProperty('error');
  });
});
