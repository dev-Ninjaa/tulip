import os from 'os';
import readline from 'readline';
import { getConfig } from '../src/config';
import {
  findOllamaExecutable,
  isOllamaServerRunning,
  ensureOllamaServerRunning,
  isModelInstalled,
  pullModel,
  verifyTextInference,
  verifyVisionInference,
  getSystemMemoryInfo,
} from '../src/ollama';
import { getCandidateIPv4Addresses, getPrimaryIPv4Address, findAvailablePort } from '../src/network';

function askConfirmation(query: string, defaultYes = true): Promise<boolean> {
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  const hint = defaultYes ? '[Y/n]' : '[y/N]';
  return new Promise((resolve) => {
    rl.question(`${query} ${hint} `, (answer) => {
      rl.close();
      const trimmed = answer.trim().toLowerCase();
      if (!trimmed) {
        resolve(defaultYes);
      } else {
        resolve(trimmed === 'y' || trimmed === 'yes');
      }
    });
  });
}

async function runSetup() {
  const config = getConfig();

  console.log('===================================================');
  console.log(' Tulip Local AI Routing Setup');
  console.log(' Target Model: ' + config.modelName);
  console.log('===================================================\n');

  // Step 1: Detect Operating System
  const platformName =
    os.platform() === 'win32' ? 'Windows' : os.platform() === 'darwin' ? 'macOS' : 'Linux';
  console.log(`[Step 1/15] Operating System Detected: ${platformName} (${os.arch()})`);

  // Step 2 & 3: Detect Ollama Installation
  let ollamaBin = findOllamaExecutable();
  console.log(`[Step 2/15] Checking Ollama installation...`);

  if (!ollamaBin) {
    console.log('\n❌ Ollama executable not found in PATH or standard directories.');
    console.log('   Official installation instructions:');
    console.log('   - Windows: Download installer from https://ollama.com/download/windows');
    console.log('   - macOS:   Download installer from https://ollama.com/download/mac or run: brew install ollama');
    console.log('   - Linux:   Run: curl -fsSL https://ollama.com/install.sh | sh\n');

    const continueAnyway = await askConfirmation('Did you install Ollama manually and want to re-check?', true);
    ollamaBin = findOllamaExecutable();
    if (!ollamaBin && !continueAnyway) {
      console.log('\nSetup aborted. Please install Ollama and run setup again.');
      process.exit(1);
    }
  } else {
    console.log(`   Ollama executable found: ${ollamaBin}`);
  }

  // Step 4: Check system memory
  const mem = getSystemMemoryInfo();
  console.log(`\n[Step 4/15] Checking System Memory: ${mem.totalGb} GB total RAM (${mem.freeGb} GB free)`);
  if (!mem.isSufficient) {
    console.log(`⚠️ Warning: Total RAM is ${mem.totalGb} GB. Gemma 3 4B recommends at least 8 GB RAM/VRAM for smooth inference.`);
    const proceed = await askConfirmation('Do you want to proceed anyway?', true);
    if (!proceed) {
      console.log('Setup canceled by user due to RAM warning.');
      process.exit(0);
    }
  }

  // Step 5: Check & start Ollama server
  console.log(`\n[Step 5/15] Checking Ollama server status on ${config.ollamaHost}...`);
  let startedByUs = false;

  try {
    const result = await ensureOllamaServerRunning(config.ollamaHost);
    startedByUs = result.startedByUs;
    console.log(`   Ollama server is active on ${config.ollamaHost}`);
  } catch (err: any) {
    console.error(`❌ Failed to start or reach Ollama server: ${err.message}`);
    process.exit(1);
  }

  // Step 6 & 7 & 8: Check and download model
  console.log(`\n[Step 6/15] Checking if model '${config.modelName}' is installed...`);
  const modelPresent = await isModelInstalled(config.modelName, config.ollamaHost);

  if (modelPresent) {
    console.log(`   Model '${config.modelName}' is already installed.`);
  } else {
    console.log(`   Model '${config.modelName}' is NOT installed on this machine.`);
    console.log('   Note: Gemma 3 4B download size is approximately ~3.3 GB.');
    const confirmDownload = await askConfirmation(`Do you want to pull '${config.modelName}' now?`, true);

    if (!confirmDownload) {
      console.log('Setup paused. You can pull the model later using: ollama pull gemma3:4b');
      process.exit(0);
    }

    try {
      await pullModel(config.modelName);
    } catch (err: any) {
      console.error(`❌ Model pull failed: ${err.message}`);
      console.log('   You can attempt downloading manually via: ollama pull gemma3:4b');
      process.exit(1);
    }
  }

  // Step 9: Wait for model and server readiness
  console.log(`\n[Step 9/15] Verifying model readiness...`);
  const ready = await isModelInstalled(config.modelName, config.ollamaHost);
  if (!ready) {
    console.error(`❌ Model '${config.modelName}' is not registered in Ollama. Aborting.`);
    process.exit(1);
  }

  // Step 10: Real text inference test
  console.log(`\n[Step 10/15] Testing real text inference with '${config.modelName}'...`);
  const textRes = await verifyTextInference(config.modelName, config.ollamaHost);
  if (textRes.success) {
    console.log(`   ✅ Text inference verified! Latency: ${textRes.latencyMs}ms`);
    console.log(`   Response sample: "${textRes.response}"`);
  } else {
    console.error(`❌ Text inference verification failed: ${textRes.response}`);
    process.exit(1);
  }

  // Step 11: Real image / vision inference test
  console.log(`\n[Step 11/15] Testing image/vision inference with '${config.modelName}'...`);
  const visionRes = await verifyVisionInference(config.modelName, config.ollamaHost);
  if (visionRes.success) {
    console.log(`   ✅ Vision inference verified! Latency: ${visionRes.latencyMs}ms`);
    console.log(`   Response sample: "${visionRes.response}"`);
  } else {
    console.warn(`   ⚠️ Vision inference check failed: ${visionRes.response}`);
    console.warn('   Text inference works, but multimodal vision capabilities may be limited on this hardware setup.');
  }

  // Step 12 & 13: Local network discovery & port selection
  console.log(`\n[Step 12-13/15] Detecting candidate LAN network interfaces...`);
  const candidates = getCandidateIPv4Addresses();
  const primaryIp = getPrimaryIPv4Address();
  const boundPort = await findAvailablePort(config.port);

  console.log(`   Selected LAN IPv4 Address: ${primaryIp}`);
  console.log(`   Selected Router Port: ${boundPort}`);

  // Step 14: Final Instructions Box
  console.log('\n===================================================');
  console.log(' 🎉 TULIP LOCAL AI ROUTING SETUP COMPLETE!');
  console.log('===================================================');
  console.log(` Target Model:  ${config.modelName}`);
  console.log(` Server Status: Ready`);
  console.log(` Router LAN URL: http://${primaryIp}:${boundPort}`);
  console.log('---------------------------------------------------');
  console.log(' Instructions for Tulip Android App:');
  console.log(' 1. Ensure phone and laptop are connected to the same Wi-Fi.');
  console.log(' 2. Open Tulip Settings -> AI Provider -> Local Ollama.');
  console.log(` 3. Enter Base URL: http://${primaryIp}:${boundPort}`);
  console.log(' 4. Tap "Test Connection" in Tulip Settings.');
  console.log('===================================================\n');

  if (candidates.length > 1) {
    console.log('Other available network interface addresses on this machine:');
    candidates.slice(1).forEach((cand) => {
      console.log(` - [${cand.name}] http://${cand.address}:${boundPort}`);
    });
    console.log('');
  }

  // Step 15: Shutdown instructions
  console.log('[Step 15/15] Next Commands:');
  console.log(' - Start the Router:   npm run start');
  console.log(' - Check Diagnostics: npm run status');
  console.log(' - Stop Services:      npm run stop\n');
}

runSetup().catch((err) => {
  console.error('Fatal error during setup:', err);
  process.exit(1);
});
