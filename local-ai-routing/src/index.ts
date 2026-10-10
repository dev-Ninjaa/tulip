import fs from 'fs';
import path from 'path';
import { getConfig } from './config';
import { createRouterApp } from './router';
import { findAvailablePort, getCandidateIPv4Addresses } from './network';
import { isOllamaServerRunning, isModelInstalled } from './ollama';

const ROUTER_PID_FILE = path.join(__dirname, '../.router-pid.json');

async function main() {
  const config = getConfig();

  console.log('===================================================');
  console.log(' Starting Tulip Local AI Routing Server');
  console.log('===================================================');

  // Verify Ollama connectivity
  const ollamaOnline = await isOllamaServerRunning(config.ollamaHost);
  if (!ollamaOnline) {
    console.warn(`[Warning] Upstream Ollama server at ${config.ollamaHost} is not currently responding.`);
    console.warn('Run "npm run setup" to start Ollama and install required models.');
  } else {
    const modelReady = await isModelInstalled(config.modelName, config.ollamaHost);
    if (!modelReady) {
      console.warn(`[Warning] Model '${config.modelName}' is not installed in local Ollama.`);
      console.warn(`Run "npm run setup" or "ollama pull ${config.modelName}".`);
    } else {
      console.log(`Upstream Ollama connected: ${config.ollamaHost}`);
      console.log(`Target Model verified: ${config.modelName}`);
    }
  }

  // Find available port starting from preferred config.port
  const boundPort = await findAvailablePort(config.port, '0.0.0.0');
  const app = createRouterApp(config);

  const server = app.listen(boundPort, '0.0.0.0', () => {
    // Record router PID
    fs.writeFileSync(
      ROUTER_PID_FILE,
      JSON.stringify({ pid: process.pid, port: boundPort, startedAt: new Date().toISOString() })
    );

    const candidates = getCandidateIPv4Addresses();
    const primaryIp = candidates.length > 0 ? candidates[0].address : '127.0.0.1';

    console.log('\n===================================================');
    console.log(' Tulip Local AI Router is READY!');
    console.log(` Target Model:  ${config.modelName}`);
    console.log(` Server URL:    http://${primaryIp}:${boundPort}`);
    console.log('---------------------------------------------------');
    console.log(' Use this URL in Tulip Settings -> AI Provider -> Local Ollama');
    console.log('===================================================\n');

    if (candidates.length > 1) {
      console.log('Additional candidate network addresses on this machine:');
      candidates.slice(1).forEach((cand) => {
        console.log(` - [${cand.name}] http://${cand.address}:${boundPort}`);
      });
      console.log('');
    }
  });

  const gracefulShutdown = (signal: string) => {
    console.log(`\nReceived ${signal}. Shutting down router server...`);
    server.close(() => {
      if (fs.existsSync(ROUTER_PID_FILE)) {
        try { fs.unlinkSync(ROUTER_PID_FILE); } catch {}
      }
      console.log('Router server stopped cleanly.');
      process.exit(0);
    });
  };

  process.on('SIGINT', () => gracefulShutdown('SIGINT'));
  process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
}

if (require.main === module) {
  main().catch((err) => {
    console.error('Fatal error starting router server:', err);
    process.exit(1);
  });
}
