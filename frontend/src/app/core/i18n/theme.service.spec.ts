import { TestBed } from '@angular/core/testing';

import { environment } from '@env/environment';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.removeItem(environment.storageKeys.theme);
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.style.removeProperty('color-scheme');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [] });
  });

  it('defaults to SYSTEM when nothing stored', () => {
    const service = TestBed.inject(ThemeService);
    expect(service.mode()).toBe('SYSTEM');
    expect(['light', 'dark']).toContain(service.resolved());
    expect(document.documentElement.getAttribute('data-theme')).toBeTruthy();
  });

  it('setMode(LIGHT) applies data-theme=light', () => {
    const service = TestBed.inject(ThemeService);
    service.setMode('LIGHT');
    expect(service.mode()).toBe('LIGHT');
    expect(service.resolved()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('setMode(DARK) applies data-theme=dark', () => {
    const service = TestBed.inject(ThemeService);
    service.setMode('DARK');
    expect(service.mode()).toBe('DARK');
    expect(service.resolved()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('persists the mode to localStorage by default', () => {
    const service = TestBed.inject(ThemeService);
    service.setMode('DARK');
    const raw = localStorage.getItem(environment.storageKeys.theme);
    expect(raw).toBeTruthy();
    expect(JSON.parse(raw!).mode).toBe('DARK');
  });

  it('does not persist when persist: false', () => {
    const service = TestBed.inject(ThemeService);
    service.setMode('LIGHT', { persist: false });
    expect(localStorage.getItem(environment.storageKeys.theme)).toBeNull();
  });

  it('toggle switches between light and dark', () => {
    const service = TestBed.inject(ThemeService);
    service.setMode('LIGHT');
    service.toggle();
    expect(service.resolved()).toBe('dark');
    service.toggle();
    expect(service.resolved()).toBe('light');
  });
});
