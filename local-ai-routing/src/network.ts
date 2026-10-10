import os from 'os';
import net from 'net';

export interface CandidateIP {
  address: string;
  name: string;
  isPreferred: boolean;
  score: number;
}

/**
 * Discovers candidate LAN IPv4 addresses suitable for mobile device connections.
 */
export function getCandidateIPv4Addresses(): CandidateIP[] {
  const interfaces = os.networkInterfaces();
  const candidates: CandidateIP[] = [];

  for (const [name, netIfs] of Object.entries(interfaces)) {
    if (!netIfs) continue;

    const lowerName = name.toLowerCase();
    const isVirtual =
      lowerName.includes('docker') ||
      lowerName.includes('vethernet') ||
      lowerName.includes('wsl') ||
      lowerName.includes('vbox') ||
      lowerName.includes('vmware') ||
      lowerName.includes('virtual') ||
      lowerName.includes('hyper-v');

    for (const netIf of netIfs) {
      // Must be IPv4 and non-internal (not 127.0.0.1)
      if (netIf.family !== 'IPv4' || netIf.internal) {
        continue;
      }

      const ip = netIf.address;

      // Exclude APIPA / link-local addresses (169.254.x.x)
      if (ip.startsWith('169.254.')) {
        continue;
      }

      // Calculate priority score
      let score = 0;
      const isPrivate =
        ip.startsWith('192.168.') ||
        ip.startsWith('10.') ||
        /^172\.(1[6-9]|2[0-9]|3[0-1])\./.test(ip);

      if (isPrivate) score += 10;
      if (!isVirtual) score += 20;
      if (lowerName.includes('wi-fi') || lowerName.includes('wifi') || lowerName.includes('wlan')) {
        score += 30;
      } else if (lowerName.includes('ethernet') || lowerName.includes('eth') || lowerName.includes('en0')) {
        score += 25;
      }

      candidates.push({
        address: ip,
        name,
        isPreferred: false,
        score,
      });
    }
  }

  // Sort candidates by score descending
  candidates.sort((a, b) => b.score - a.score);

  if (candidates.length > 0) {
    candidates[0].isPreferred = true;
  }

  return candidates;
}

/**
 * Returns the primary/preferred LAN IPv4 address or fallback to 127.0.0.1 if none found.
 */
export function getPrimaryIPv4Address(): string {
  const candidates = getCandidateIPv4Addresses();
  return candidates.length > 0 ? candidates[0].address : '127.0.0.1';
}

/**
 * Finds an available TCP port starting from preferredPort.
 */
export function findAvailablePort(preferredPort: number, host = '0.0.0.0'): Promise<number> {
  return new Promise((resolve, reject) => {
    let port = preferredPort;

    const checkPort = (testPort: number) => {
      if (testPort > preferredPort + 100) {
        return reject(new Error(`Could not find an available port in range ${preferredPort}-${preferredPort + 100}`));
      }

      const server = net.createServer();
      server.unref();

      server.on('error', (err: any) => {
        if (err.code === 'EADDRINUSE' || err.code === 'EACCES') {
          checkPort(testPort + 1);
        } else {
          reject(err);
        }
      });

      server.listen(testPort, host, () => {
        server.close(() => {
          resolve(testPort);
        });
      });
    };

    checkPort(port);
  });
}
