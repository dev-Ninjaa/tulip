import dotenv from 'dotenv';
import path from 'path';

// Load .env if present
dotenv.config({ path: path.join(__dirname, '../.env') });

export interface AppConfig {
  port: number;
  ollamaHost: string;
  modelName: string;
  maxBodySize: string;
  requestTimeoutMs: number;
  pairingToken: string;
}

export function getConfig(): AppConfig {
  const port = parseInt(process.env.PORT || '8787', 10);
  const ollamaHost = (process.env.OLLAMA_HOST || 'http://127.0.0.1:11434').replace(/\/+$/, '');
  const modelName = process.env.MODEL_NAME || 'gemma3:4b';
  const maxBodySize = process.env.MAX_BODY_SIZE || '20mb';
  const requestTimeoutMs = parseInt(process.env.REQUEST_TIMEOUT_MS || '120000', 10);
  const pairingToken = process.env.PAIRING_TOKEN || '';

  return {
    port: isNaN(port) ? 8787 : port,
    ollamaHost,
    modelName,
    maxBodySize,
    requestTimeoutMs: isNaN(requestTimeoutMs) ? 120000 : requestTimeoutMs,
    pairingToken,
  };
}
