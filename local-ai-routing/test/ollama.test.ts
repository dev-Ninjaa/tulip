import { describe, it, expect } from 'vitest';
import { findOllamaExecutable, getSystemMemoryInfo } from '../src/ollama';

describe('Ollama Helper Module', () => {
  it('findOllamaExecutable returns string or null', () => {
    const exe = findOllamaExecutable();
    if (exe !== null) {
      expect(typeof exe).toBe('string');
      expect(exe.length).toBeGreaterThan(0);
    } else {
      expect(exe).toBeNull();
    }
  });

  it('getSystemMemoryInfo returns memory details', () => {
    const mem = getSystemMemoryInfo();
    expect(mem.totalGb).toBeGreaterThan(0);
    expect(mem.freeGb).toBeGreaterThanOrEqual(0);
    expect(typeof mem.isSufficient).toBe('boolean');
  });
});
