import { ErrorHandler, Injectable, inject } from '@angular/core';

import { LoggerService } from '@core/observability/logger.service';

@Injectable({ providedIn: 'root' })
export class PortalErrorHandler implements ErrorHandler {
  private readonly logger = inject(LoggerService);

  handleError(error: unknown): void {
    const message = error instanceof Error ? error.message : String(error);
    const stack = error instanceof Error ? error.stack : undefined;
    this.logger.error('unhandled.error', { message, stack });
    if (typeof console !== 'undefined') {
      console.error(error);
    }
  }
}
