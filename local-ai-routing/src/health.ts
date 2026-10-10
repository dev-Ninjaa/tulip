import { AppConfig, getConfig } from './config';
import { findOllamaExecutable, isOllamaServerRunning, isModelInstalled, verifyTextInference, verifyVisionInference } from './ollama';
import { getCandidateIPv4Addresses, getPrimaryIPv4Address } from './network';
import http from 'http';

export interface SystemStatusReport {
  timestamp: string;
  os: string;
  ollamaExecutable: string | null;
  ollamaServerRunning: boolean;
  modelInstalled: boolean;
  textInferenceVerified: boolean;
  textInferenceLatencyMs?: number;
  textResponseSnippet?: string;
  visionInferenceVerified: boolean;
  visionInferenceLatencyMs?: number;
  visionResponseSnippet?: string;
  routerRunning: boolean;
  primaryLanUrl: string | null;
  candidateAddresses: string[];
}

export async function runSystemDiagnostic(customConfig?: AppConfig): Promise<SystemStatusReport> {
  const config = customConfig || getConfig();
  const osType = `${process.platform} (${process.arch})`;
  const ollamaBin = findOllamaExecutable();
  const ollamaOnline = await isOllamaServerRunning(config.ollamaHost);
  const modelReady = ollamaOnline ? await isModelInstalled(config.modelName, config.ollamaHost) : false;

  let textVerified = false;
  let textLatencyMs: number | undefined;
  let textSnippet: string | undefined;

  if (ollamaOnline && modelReady) {
    const textCheck = await verifyTextInference(config.modelName, config.ollamaHost);
    textVerified = textCheck.success;
    textLatencyMs = textCheck.latencyMs;
    textSnippet = textCheck.response;
  }

  let visionVerified = false;
  let visionLatencyMs: number | undefined;
  let visionSnippet: string | undefined;

  if (ollamaOnline && modelReady && textVerified) {
    const visionCheck = await verifyVisionInference(config.modelName, config.ollamaHost);
    visionVerified = visionCheck.success;
    visionLatencyMs = visionCheck.latencyMs;
    visionSnippet = visionCheck.response;
  }

  // Check if router is currently running on config.port
  const routerOnline = await checkRouterPortOnline(config.port);

  const candidateIps = getCandidateIPv4Addresses();
  const primaryIp = getPrimaryIPv4Address();
  const primaryLanUrl = primaryIp ? `http://${primaryIp}:${config.port}` : null;

  return {
    timestamp: new Date().toISOString(),
    os: osType,
    ollamaExecutable: ollamaBin,
    ollamaServerRunning: ollamaOnline,
    modelInstalled: modelReady,
    textInferenceVerified: textVerified,
    textInferenceLatencyMs: textLatencyMs,
    textResponseSnippet: textSnippet,
    visionInferenceVerified: visionVerified,
    visionInferenceLatencyMs: visionLatencyMs,
    visionResponseSnippet: visionSnippet,
    routerRunning: routerOnline,
    primaryLanUrl,
    candidateAddresses: candidateIps.map((c) => `${c.name}: http://${c.address}:${config.port}`),
  };
}

function checkRouterPortOnline(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const req = http.get(`http://127.0.0.1:${port}/health`, { timeout: 2000 }, (res) => {
      resolve(res.statusCode === 200);
    });
    req.on('error', () => resolve(false));
    req.on('timeout', () => {
      req.destroy();
      resolve(false);
    });
  });
}
