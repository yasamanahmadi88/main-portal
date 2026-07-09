import { Injectable } from '@angular/core';

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

@Injectable({ providedIn: 'root' })
export class LoggerService {
  private readonly production =
    typeof globalThis !== 'undefined' && (globalThis as any).__PORTAL_PROD__ === true;

  debug(message: string, meta?: Record<string, unknown>): void {
    if (!this.production) {
      this.emit('debug', message, meta);
    }
  }

  info(message: string, meta?: Record<string, unknown>): void {
    this.emit('info', message, meta);
  }

  warn(message: string, meta?: Record<string, unknown>): void {
    this.emit('warn', message, meta);
  }

  error(message: string, meta?: Record<string, unknown>): void {
    this.emit('error', message, meta);
  }

  private emit(level: LogLevel, message: string, meta?: Record<string, unknown>): void {
    const payload = { level, message, ts: new Date().toISOString(), ...meta };
    const fn: (...args: unknown[]) => void =
      level === 'error'
        ? console.error
        : level === 'warn'
          ? console.warn
          : level === 'info'
            ? console.info
            : console.debug;
    try {
      fn.call(console, `[portal:${level}]`, payload);
    } catch {
      /* console can be replaced by iframe hosts; ignore */
    }
  }
}
