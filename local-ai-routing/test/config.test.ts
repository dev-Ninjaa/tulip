import { describe, it, expect } from 'vitest';
import { getConfig } from '../src/config';

describe('Config Module', () => {
  it('should load default configuration settings', () => {
    const config = getConfig();
    expect(config.port).toBe(8787);
    expect(config.ollamaHost).toBe('http://127.0.0.1:11434');
    expect(config.modelName).toBe('gemma3:4b');
    expect(config.maxBodySize).toBe('20mb');
    expect(config.requestTimeoutMs).toBe(120000);
  });
});
