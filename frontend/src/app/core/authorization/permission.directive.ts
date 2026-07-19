import {
  Directive,
  Input,
  TemplateRef,
  ViewContainerRef,
  computed,
  effect,
  inject,
  signal
} from '@angular/core';

import { AuthService } from '@core/authentication/auth.service';

/**
 * Structural directive that renders a template only when the current user
 * has the specified permission(s).
 *
 * Example:
 *   <button *appHasPermission="'USER_CREATE'">Add</button>
 *   <ng-container *appHasPermission="['USER_UPDATE','USER_DELETE']; mode: 'ALL'">
 */
@Directive({
  selector: '[appHasPermission]',
  standalone: true
})
export class HasPermissionDirective {
  private readonly template = inject(TemplateRef<unknown>);
  private readonly viewContainer = inject(ViewContainerRef);
  private readonly auth = inject(AuthService);

  private readonly _codes = signal<readonly string[]>([]);
  private readonly _mode = signal<'ANY' | 'ALL'>('ANY');

  private readonly show = computed(() => {
    const codes = this._codes();
    if (codes.length === 0) return true;
    return this._mode() === 'ALL'
      ? this.auth.hasAllPermissions(codes)
      : this.auth.hasAnyPermission(codes);
  });

  constructor() {
    effect(() => {
      const visible = this.show();
      const hasView = this.viewContainer.length > 0;
      if (visible && !hasView) {
        this.viewContainer.createEmbeddedView(this.template);
      } else if (!visible && hasView) {
        this.viewContainer.clear();
      }
    });
  }

  @Input({ required: true })
  set appHasPermission(value: string | readonly string[]) {
    this._codes.set(Array.isArray(value) ? value : [value as string]);
  }

  @Input()
  set appHasPermissionMode(value: 'ANY' | 'ALL') {
    this._mode.set(value);
  }
}
