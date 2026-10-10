import fs from 'fs';
import path from 'path';
import { getConfig } from '../src/config';
import { verifyTextInference, verifyVisionInference } from '../src/ollama';

async function runInferenceTest() {
  const config = getConfig();
  const args = process.argv.slice(2);

  const isVision = args.includes('--vision') || args.includes('--image');
  let customBase64: string | undefined;

  const imageIdx = args.indexOf('--image');
  if (imageIdx !== -1 && args[imageIdx + 1]) {
    const imagePath = path.resolve(args[imageIdx + 1]);
    if (fs.existsSync(imagePath)) {
      console.log(`Loading image from file: ${imagePath}`);
      customBase64 = fs.readFileSync(imagePath).toString('base64');
    } else {
      console.error(`Image file not found: ${imagePath}`);
      process.exit(1);
    }
  }

  console.log('===================================================');
  console.log(` Testing Local Inference with Model: ${config.modelName}`);
  console.log(` Host: ${config.ollamaHost}`);
  console.log('===================================================\n');

  if (isVision) {
    console.log('Running Vision / Multimodal Inference Test...');
    const result = await verifyVisionInference(config.modelName, config.ollamaHost, customBase64);
    if (result.success) {
      console.log(`✅ Vision Inference PASSED! (${result.latencyMs}ms)`);
      console.log(`Response: ${result.response}`);
    } else {
      console.error(`❌ Vision Inference FAILED: ${result.response}`);
      process.exit(1);
    }
  } else {
    console.log('Running Text Inference Test...');
    const result = await verifyTextInference(config.modelName, config.ollamaHost);
    if (result.success) {
      console.log(`✅ Text Inference PASSED! (${result.latencyMs}ms)`);
      console.log(`Response: ${result.response}`);
    } else {
      console.error(`❌ Text Inference FAILED: ${result.response}`);
      process.exit(1);
    }
  }
}

runInferenceTest().catch((err) => {
  console.error('Inference test execution error:', err);
  process.exit(1);
});
