import { runSystemDiagnostic } from '../src/health';
import { getConfig } from '../src/config';

async function runStatus() {
  const config = getConfig();
  console.log('===================================================');
  console.log(' Tulip Local AI System Status');
  console.log('===================================================');

  const report = await runSystemDiagnostic(config);

  console.log(` Timestamp:             ${report.timestamp}`);
  console.log(` OS Platform:           ${report.os}`);
  console.log(` Ollama Executable:     ${report.ollamaExecutable ? report.ollamaExecutable : '❌ NOT FOUND'}`);
  console.log(` Ollama API Status:     ${report.ollamaServerRunning ? '✅ RESPONDING' : '❌ UNREACHABLE'}`);
  console.log(` Model '${config.modelName}':   ${report.modelInstalled ? '✅ INSTALLED' : '❌ NOT INSTALLED'}`);

  console.log(` Text Inference Test:   ${report.textInferenceVerified ? `✅ PASSED (${report.textInferenceLatencyMs}ms)` : '❌ FAILED'}`);
  if (report.textResponseSnippet) {
    console.log(`   Text Output: "${report.textResponseSnippet}"`);
  }

  console.log(` Vision Inference Test: ${report.visionInferenceVerified ? `✅ PASSED (${report.visionInferenceLatencyMs}ms)` : '⚠️ NOT VERIFIED / FAILED'}`);
  if (report.visionResponseSnippet) {
    console.log(`   Vision Output: "${report.visionResponseSnippet}"`);
  }

  console.log(` Router Server:         ${report.routerRunning ? '✅ RUNNING' : '⚪ STOPPED'}`);
  console.log('---------------------------------------------------');
  console.log(` Primary LAN URL:       ${report.primaryLanUrl ? report.primaryLanUrl : 'None detected'}`);
  console.log('---------------------------------------------------');

  if (report.candidateAddresses.length > 0) {
    console.log('Candidate Interfaces:');
    report.candidateAddresses.forEach((addr) => console.log(` - ${addr}`));
  }

  console.log('===================================================\n');

  if (!report.ollamaServerRunning || !report.modelInstalled || !report.textInferenceVerified) {
    process.exitCode = 1;
  }
}

runStatus().catch((err) => {
  console.error('Error getting system status:', err);
  process.exit(1);
});
