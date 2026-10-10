import express, { Request, Response, NextFunction } from 'express';
import http from 'http';
import https from 'https';
import { AppConfig } from './config';
import { isOllamaServerRunning, isModelInstalled } from './ollama';

export function createRouterApp(config: AppConfig) {
  const app = express();

  // Configure body parser limits for high-res images
  app.use(express.json({ limit: config.maxBodySize }));
  app.use(express.urlencoded({ extended: true, limit: config.maxBodySize }));

  // Sanitize and log requests cleanly without dumping image/prompt payloads
  app.use((req: Request, res: Response, next: NextFunction) => {
    const start = Date.now();
    const { method, path: reqPath } = req;

    res.on('finish', () => {
      const duration = Date.now() - start;
      console.log(`[Router] ${method} ${reqPath} -> ${res.statusCode} (${duration}ms)`);
    });

    next();
  });

  // Pairing token validation middleware (if token is set)
  app.use((req: Request, res: Response, next: NextFunction) => {
    if (config.pairingToken) {
      const headerToken = req.headers['x-tulip-pairing-token'] || req.headers['authorization'];
      const queryToken = req.query.pairing_token;

      if (headerToken !== config.pairingToken && queryToken !== config.pairingToken) {
        res.status(401).json({ error: 'Unauthorized: Invalid pairing token' });
        return;
      }
    }
    next();
  });

  // Health Endpoint
  app.get('/health', async (req: Request, res: Response) => {
    const ollamaOnline = await isOllamaServerRunning(config.ollamaHost);
    const modelReady = ollamaOnline ? await isModelInstalled(config.modelName, config.ollamaHost) : false;

    res.json({
      status: ollamaOnline && modelReady ? 'healthy' : 'degraded',
      router: 'online',
      ollamaHost: config.ollamaHost,
      ollamaOnline,
      targetModel: config.modelName,
      modelInstalled: modelReady,
      timestamp: new Date().toISOString(),
    });
  });

  // Proxy GET /api/tags
  app.get('/api/tags', async (req: Request, res: Response) => {
    forwardToOllama('GET', '/api/tags', null, req, res, config);
  });

  // Proxy GET /api/version
  app.get('/api/version', async (req: Request, res: Response) => {
    forwardToOllama('GET', '/api/version', null, req, res, config);
  });

  // Proxy POST /api/chat
  app.post('/api/chat', async (req: Request, res: Response) => {
    const body = req.body || {};
    const requestedModel = body.model || config.modelName;

    // Check if model is available before proxying
    const ollamaOnline = await isOllamaServerRunning(config.ollamaHost);
    if (!ollamaOnline) {
      res.status(503).json({
        error: `Local Ollama server at ${config.ollamaHost} is unreachable. Ensure Ollama is running.`
      });
      return;
    }

    const modelInstalled = await isModelInstalled(requestedModel, config.ollamaHost);
    if (!modelInstalled) {
      res.status(404).json({
        error: `Requested model '${requestedModel}' is not installed in local Ollama instance.`
      });
      return;
    }

    forwardToOllama('POST', '/api/chat', body, req, res, config);
  });

  // Catch-all 404 handler for unsupported endpoints
  app.use((req: Request, res: Response) => {
    res.status(404).json({
      error: `Endpoint '${req.method} ${req.path}' is not supported by Tulip Local AI Router.`
    });
  });

  return app;
}

function forwardToOllama(
  method: string,
  path: string,
  payload: any,
  req: Request,
  res: Response,
  config: AppConfig
) {
  try {
    const targetUrl = new URL(path, config.ollamaHost);
    const client = targetUrl.protocol === 'https:' ? https : http;

    const payloadBuffer = payload ? Buffer.from(JSON.stringify(payload)) : null;

    const headers: Record<string, string | string[] | undefined> = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    if (payloadBuffer) {
      headers['Content-Length'] = String(payloadBuffer.length);
    }

    const proxyReq = client.request(
      targetUrl.toString(),
      {
        method,
        headers,
        timeout: config.requestTimeoutMs,
      },
      (ollamaRes) => {
        res.status(ollamaRes.statusCode || 500);

        // Copy content type and other response headers
        if (ollamaRes.headers['content-type']) {
          res.setHeader('Content-Type', ollamaRes.headers['content-type']);
        }

        ollamaRes.pipe(res);
      }
    );

    proxyReq.on('error', (err: any) => {
      console.error(`[Router Error] Failed to proxy ${method} ${path} to Ollama:`, err.message);
      if (!res.headersSent) {
        res.status(502).json({
          error: `Bad Gateway: Unable to connect to Ollama server at ${config.ollamaHost}`,
          details: err.message,
        });
      }
    });

    proxyReq.on('timeout', () => {
      proxyReq.destroy();
      if (!res.headersSent) {
        res.status(504).json({
          error: `Gateway Timeout: Request to Ollama timed out after ${config.requestTimeoutMs}ms`,
        });
      }
    });

    if (payloadBuffer) {
      proxyReq.write(payloadBuffer);
    }
    proxyReq.end();
  } catch (err: any) {
    if (!res.headersSent) {
      res.status(500).json({ error: 'Internal Router Error', details: err.message });
    }
  }
}
