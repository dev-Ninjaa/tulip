import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const ROUTER_PID_FILE = path.join(__dirname, '../.router-pid.json');
const OLLAMA_PID_FILE = path.join(__dirname, '../.ollama-managed-pid.json');

function killPid(pid: number, label: string) {
  try {
    console.log(`Stopping ${label} (PID: ${pid})...`);
    if (process.platform === 'win32') {
      execSync(`taskkill /F /PID ${pid}`, { stdio: 'ignore' });
    } else {
      process.kill(pid, 'SIGTERM');
    }
    console.log(`Successfully stopped ${label} (PID: ${pid}).`);
  } catch (err: any) {
    console.warn(`Could not terminate ${label} PID ${pid} (may have already stopped): ${err.message}`);
  }
}

function runStop() {
  console.log('===================================================');
  console.log(' Stopping Tulip Local AI Routing Services');
  console.log('===================================================');

  let stoppedAnything = false;

  // 1. Stop router if PID file exists
  if (fs.existsSync(ROUTER_PID_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(ROUTER_PID_FILE, 'utf8'));
      if (data.pid) {
        killPid(data.pid, 'Tulip Router Server');
        stoppedAnything = true;
      }
    } catch {}
    try { fs.unlinkSync(ROUTER_PID_FILE); } catch {}
  } else {
    console.log('No active Tulip Router Server PID file found.');
  }

  // 2. Stop launcher-managed Ollama process ONLY if started by us
  if (fs.existsSync(OLLAMA_PID_FILE)) {
    try {
      const data = JSON.parse(fs.readFileSync(OLLAMA_PID_FILE, 'utf8'));
      if (data.pid) {
        killPid(data.pid, 'Launcher-managed Ollama Instance');
        stoppedAnything = true;
      }
    } catch {}
    try { fs.unlinkSync(OLLAMA_PID_FILE); } catch {}
  } else {
    console.log('No launcher-managed Ollama server PID file found. Pre-existing Ollama services remain running.');
  }

  if (!stoppedAnything) {
    console.log('No services were running under launcher tracking.');
  }

  console.log('Stop sequence complete.\n');
}

runStop();
