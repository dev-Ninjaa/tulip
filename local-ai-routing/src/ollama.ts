import fs from 'fs';
import path from 'path';
import os from 'os';
import { spawn, execSync } from 'child_process';
import http from 'http';
import https from 'https';

export interface ModelTag {
  name: string;
  modified_at: string;
  size: number;
  digest: string;
}

export interface TagsResponse {
  models: ModelTag[];
}

export interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
  images?: string[];
}

export interface ChatResponse {
  model: string;
  created_at: string;
  message: ChatMessage;
  done: boolean;
  total_duration?: number;
  load_duration?: number;
  prompt_eval_count?: number;
  eval_count?: number;
}

const PID_FILE = path.join(__dirname, '../.ollama-managed-pid.json');

/**
 * Searches for the Ollama executable across system PATH and default installation locations.
 */
export function findOllamaExecutable(): string | null {
  // First check if 'ollama' is in PATH
  try {
    const cmd = os.platform() === 'win32' ? 'where ollama' : 'which ollama';
    const output = execSync(cmd, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
    if (output) {
      const firstLine = output.split(/\r?\n/)[0].trim();
      if (fs.existsSync(firstLine)) return firstLine;
    }
  } catch (e) {
    // ignore
  }

  // OS specific candidate paths
  const candidates: string[] = [];
  if (os.platform() === 'win32') {
    const localAppData = process.env.LOCALAPPDATA || path.join(os.homedir(), 'AppData', 'Local');
    const programFiles = process.env.ProgramFiles || 'C:\\Program Files';
    candidates.push(path.join(localAppData, 'Programs', 'Ollama', 'ollama.exe'));
    candidates.push(path.join(programFiles, 'Ollama', 'ollama.exe'));
  } else if (os.platform() === 'darwin') {
    candidates.push('/usr/local/bin/ollama');
    candidates.push('/opt/homebrew/bin/ollama');
    candidates.push('/Applications/Ollama.app/Contents/Resources/ollama');
  } else {
    candidates.push('/usr/bin/ollama');
    candidates.push('/usr/local/bin/ollama');
    candidates.push(path.join(os.homedir(), '.ollama', 'ollama'));
  }

  for (const cand of candidates) {
    if (fs.existsSync(cand)) {
      return cand;
    }
  }

  return null;
}

/**
 * Performs a lightweight HTTP check to verify if Ollama server is responding at ollamaHost.
 */
export async function isOllamaServerRunning(ollamaHost: string = 'http://127.0.0.1:11434'): Promise<boolean> {
  try {
    const url = new URL('/api/tags', ollamaHost);
    const client = url.protocol === 'https:' ? https : http;

    return await new Promise<boolean>((resolve) => {
      const req = client.get(url.toString(), { timeout: 3000 }, (res) => {
        resolve(res.statusCode === 200);
      });
      req.on('error', () => resolve(false));
      req.on('timeout', () => {
        req.destroy();
        resolve(false);
      });
    });
  } catch {
    return false;
  }
}

/**
 * Checks system RAM in GB.
 */
export function getSystemMemoryInfo() {
  const totalGb = Math.round((os.totalmem() / (1024 * 1024 * 1024)) * 10) / 10;
  const freeGb = Math.round((os.freemem() / (1024 * 1024 * 1024)) * 10) / 10;
  return { totalGb, freeGb, isSufficient: totalGb >= 8 };
}

/**
 * Spawns Ollama server in background if not already running.
 * Saves child PID to .ollama-managed-pid.json so stop script can clean it up safely.
 */
export async function ensureOllamaServerRunning(ollamaHost: string = 'http://127.0.0.1:11434'): Promise<{ startedByUs: boolean; host: string }> {
  if (await isOllamaServerRunning(ollamaHost)) {
    return { startedByUs: false, host: ollamaHost };
  }

  const ollamaBin = findOllamaExecutable();
  if (!ollamaBin) {
    throw new Error('Ollama executable not found on host machine. Please install Ollama from https://ollama.com/download');
  }

  console.log(`Starting Ollama server using executable: ${ollamaBin}...`);
  const child = spawn(ollamaBin, ['serve'], {
    detached: true,
    stdio: 'ignore',
    env: process.env,
  });

  child.unref();

  if (child.pid) {
    fs.writeFileSync(PID_FILE, JSON.stringify({ pid: child.pid, startedAt: new Date().toISOString() }));
  }

  // Poll server readiness for up to 15 seconds
  const startTime = Date.now();
  while (Date.now() - startTime < 15000) {
    if (await isOllamaServerRunning(ollamaHost)) {
      console.log('Ollama server is now running and responsive.');
      return { startedByUs: true, host: ollamaHost };
    }
    await new Promise((r) => setTimeout(r, 1000));
  }

  throw new Error('Ollama server process was spawned but failed to respond on ' + ollamaHost + ' within 15 seconds.');
}

/**
 * Checks if a specific model (e.g. gemma3:4b) is installed in Ollama.
 */
export async function isModelInstalled(modelName: string, ollamaHost: string = 'http://127.0.0.1:11434'): Promise<boolean> {
  try {
    const url = new URL('/api/tags', ollamaHost);
    const client = url.protocol === 'https:' ? https : http;

    const data = await new Promise<string>((resolve, reject) => {
      const req = client.get(url.toString(), { timeout: 5000 }, (res) => {
        let body = '';
        res.on('data', (chunk) => (body += chunk));
        res.on('end', () => resolve(body));
      });
      req.on('error', reject);
      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Timeout fetching models'));
      });
    });

    const tagsResponse: TagsResponse = JSON.parse(data);
    const normalize = (n: string) => n.split(':')[0].toLowerCase();
    const targetBase = normalize(modelName);

    return tagsResponse.models?.some((m) => {
      const fullName = m.name.toLowerCase();
      return fullName === modelName.toLowerCase() || normalize(fullName) === targetBase;
    }) ?? false;
  } catch {
    return false;
  }
}

/**
 * Pulls the requested model using Ollama CLI.
 */
export async function pullModel(modelName: string): Promise<void> {
  const ollamaBin = findOllamaExecutable();
  if (!ollamaBin) {
    throw new Error('Ollama executable not found.');
  }

  console.log(`Pulling model '${modelName}' using Ollama CLI. This may take several minutes depending on network speed...`);

  return new Promise((resolve, reject) => {
    const child = spawn(ollamaBin, ['pull', modelName], {
      stdio: 'inherit',
      env: process.env,
    });

    child.on('close', (code) => {
      if (code === 0) {
        console.log(`Model '${modelName}' downloaded successfully.`);
        resolve();
      } else {
        reject(new Error(`ollama pull '${modelName}' failed with exit code ${code}`));
      }
    });

    child.on('error', (err) => reject(err));
  });
}

/**
 * Performs a real text inference verification call against Ollama.
 */
export async function verifyTextInference(modelName: string, ollamaHost: string = 'http://127.0.0.1:11434'): Promise<{ success: boolean; response: string; latencyMs: number }> {
  const startTime = Date.now();
  const payload = JSON.stringify({
    model: modelName,
    messages: [
      { role: 'user', content: 'Say hello in 5 words or fewer.' }
    ],
    stream: false,
    options: {
      temperature: 0.2,
      num_predict: 20
    }
  });

  try {
    const url = new URL('/api/chat', ollamaHost);
    const client = url.protocol === 'https:' ? https : http;

    const body = await new Promise<string>((resolve, reject) => {
      const req = client.request(
        url.toString(),
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(payload),
          },
          timeout: 45000,
        },
        (res) => {
          let resBody = '';
          res.on('data', (chunk) => (resBody += chunk));
          res.on('end', () => {
            if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
              resolve(resBody);
            } else {
              reject(new Error(`Ollama text inference returned status ${res.statusCode}: ${resBody}`));
            }
          });
        }
      );

      req.on('error', reject);
      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Text inference verification request timed out after 45s.'));
      });

      req.write(payload);
      req.end();
    });

    const result: ChatResponse = JSON.parse(body);
    const text = result.message?.content?.trim() || '';
    const latencyMs = Date.now() - startTime;

    if (!text) {
      return { success: false, response: 'Received empty response from model', latencyMs };
    }

    return { success: true, response: text, latencyMs };
  } catch (err: any) {
    return { success: false, response: err.message || String(err), latencyMs: Date.now() - startTime };
  }
}

/**
 * Performs a real image/vision inference verification call against Ollama.
 * Uses a small 1x1 test PNG base64 if no custom image is supplied.
 */
export async function verifyVisionInference(
  modelName: string,
  ollamaHost: string = 'http://127.0.0.1:11434',
  customBase64Image?: string
): Promise<{ success: boolean; response: string; latencyMs: number }> {
  const startTime = Date.now();

  // 1x1 red PNG base64
  const sampleBase64 = customBase64Image || 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';

  const payload = JSON.stringify({
    model: modelName,
    messages: [
      {
        role: 'user',
        content: 'Describe what you see in this image briefly.',
        images: [sampleBase64]
      }
    ],
    stream: false,
    options: {
      temperature: 0.2,
      num_predict: 25
    }
  });

  try {
    const url = new URL('/api/chat', ollamaHost);
    const client = url.protocol === 'https:' ? https : http;

    const body = await new Promise<string>((resolve, reject) => {
      const req = client.request(
        url.toString(),
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(payload),
          },
          timeout: 60000,
        },
        (res) => {
          let resBody = '';
          res.on('data', (chunk) => (resBody += chunk));
          res.on('end', () => {
            if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
              resolve(resBody);
            } else {
              reject(new Error(`Ollama vision inference returned status ${res.statusCode}: ${resBody}`));
            }
          });
        }
      );

      req.on('error', reject);
      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Vision inference verification request timed out after 60s.'));
      });

      req.write(payload);
      req.end();
    });

    const result: ChatResponse = JSON.parse(body);
    const text = result.message?.content?.trim() || '';
    const latencyMs = Date.now() - startTime;

    if (!text) {
      return { success: false, response: 'Received empty vision response from model', latencyMs };
    }

    return { success: true, response: text, latencyMs };
  } catch (err: any) {
    return { success: false, response: err.message || String(err), latencyMs: Date.now() - startTime };
  }
}
