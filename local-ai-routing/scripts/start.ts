import { getConfig } from '../src/config';
import { ensureOllamaServerRunning, isModelInstalled } from '../src/ollama';
import { spawn } from 'child_process';
import path from 'path';

async function runStart() {
  const config = getConfig();

  console.log('Validating Ollama dependencies before launching router server...');

  try {
    await ensureOllamaServerRunning(config.ollamaHost);
  } catch (err: any) {
    console.error(`Cannot start router: Ollama server is unreachable on ${config.ollamaHost}. Error: ${err.message}`);
    console.error('Run "npm run setup" first.');
    process.exit(1);
  }

  const modelReady = await isModelInstalled(config.modelName, config.ollamaHost);
  if (!modelReady) {
    console.error(`Cannot start router: Model '${config.modelName}' is not installed.`);
    console.error(`Run "npm run setup" or "ollama pull ${config.modelName}".`);
    process.exit(1);
  }

  // Execute index.ts using ts-node
  const indexTs = path.join(__dirname, '../src/index.ts');
  const child = spawn(process.execPath, ['-r', 'ts-node/register', indexTs], {
    stdio: 'inherit',
    env: process.env,
  });

  child.on('error', (err) => {
    console.error('Failed to launch router process:', err);
    process.exit(1);
  });
}

runStart().catch((err) => {
  console.error('Startup error:', err);
  process.exit(1);
});
