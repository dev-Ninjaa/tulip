import { describe, it, expect } from 'vitest';
import { getCandidateIPv4Addresses, getPrimaryIPv4Address, findAvailablePort } from '../src/network';

describe('Network Discovery Module', () => {
  it('should discover candidate IPv4 addresses', () => {
    const candidates = getCandidateIPv4Addresses();
    expect(Array.isArray(candidates)).toBe(true);

    candidates.forEach((cand) => {
      expect(cand.address).toMatch(/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/);
      expect(cand.address).not.toBe('127.0.0.1');
      expect(cand.address.startsWith('169.254.')).toBe(false);
    });
  });

  it('should return a non-empty primary IPv4 address', () => {
    const primary = getPrimaryIPv4Address();
    expect(typeof primary).toBe('string');
    expect(primary.length).toBeGreaterThan(0);
  });

  it('should find an available port', async () => {
    const port = await findAvailablePort(9000);
    expect(typeof port).toBe('number');
    expect(port).toBeGreaterThanOrEqual(9000);
  });
});
