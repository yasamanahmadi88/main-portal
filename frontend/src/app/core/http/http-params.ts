import { HttpParams } from '@angular/common/http';

export function toHttpParams(
  params: Record<string, unknown> | undefined | null
): HttpParams {
  let httpParams = new HttpParams();
  if (!params) return httpParams;
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') continue;
    if (Array.isArray(value)) {
      for (const item of value) {
        if (item !== undefined && item !== null && item !== '') {
          httpParams = httpParams.append(key, String(item));
        }
      }
    } else if (value instanceof Date) {
      httpParams = httpParams.set(key, value.toISOString());
    } else {
      httpParams = httpParams.set(key, String(value));
    }
  }
  return httpParams;
}
