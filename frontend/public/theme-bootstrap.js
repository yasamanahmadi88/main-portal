/**
 * Early theme / language bootstrap (runs before Angular).
 * Kept as an external file so CSP script-src 'self' can allow it.
 */
(function () {
  try {
    var stored = localStorage.getItem('portal.theme');
    var pref = stored ? JSON.parse(stored) : null;
    var mode = pref && pref.mode ? pref.mode : 'SYSTEM';
    var isDark =
      mode === 'DARK' ||
      (mode === 'SYSTEM' &&
        window.matchMedia &&
        window.matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
    var lang = localStorage.getItem('portal.lang');
    if (lang === 'en-US') {
      document.documentElement.setAttribute('lang', 'en-US');
      document.documentElement.setAttribute('dir', 'ltr');
    } else {
      document.documentElement.setAttribute('lang', 'fa-IR');
      document.documentElement.setAttribute('dir', 'rtl');
    }
  } catch (_) {
    document.documentElement.setAttribute('data-theme', 'light');
  }
})();
