import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

/**
 * Ensures the two supported languages have matching keys across every
 * namespace. If a translator adds a key to en but forgets fa (or vice
 * versa) the missing key surfaces here before it ever reaches the UI.
 */
function readNamespace(namespace: string, lang: string): Record<string, unknown> {
  const path = join(__dirname, namespace, `${lang}.json`);
  return JSON.parse(readFileSync(path, 'utf8'));
}

function flatten(obj: unknown, prefix = ''): string[] {
  if (obj === null || typeof obj !== 'object') {
    return [prefix.slice(0, -1)];
  }
  const keys: string[] = [];
  for (const [k, v] of Object.entries(obj as Record<string, unknown>)) {
    if (v !== null && typeof v === 'object' && !Array.isArray(v)) {
      keys.push(...flatten(v, `${prefix}${k}.`));
    } else {
      keys.push(`${prefix}${k}`);
    }
  }
  return keys.sort();
}

describe('i18n parity', () => {
  const namespaces = readdirSync(__dirname).filter((entry) => {
    try {
      readFileSync(join(__dirname, entry, 'en-US.json'));
      return true;
    } catch {
      return false;
    }
  });

  for (const ns of namespaces) {
    it(`${ns}: en-US and fa-IR share the same keys`, () => {
      const en = flatten(readNamespace(ns, 'en-US'));
      const fa = flatten(readNamespace(ns, 'fa-IR'));
      const missingInFa = en.filter((k) => !fa.includes(k));
      const missingInEn = fa.filter((k) => !en.includes(k));
      expect(missingInFa, `keys missing in fa-IR/${ns}: ${missingInFa.join(', ')}`).toEqual([]);
      expect(missingInEn, `keys missing in en-US/${ns}: ${missingInEn.join(', ')}`).toEqual([]);
    });
  }
});
