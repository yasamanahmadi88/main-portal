import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  ViewChild,
  forwardRef,
  inject,
  input,
  signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-password-input',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatFormFieldModule, MatIconModule, MatInputModule, MatTooltipModule, TranslatePipe],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => PasswordInputComponent)
    }
  ],
  template: `
    <mat-form-field appearance="outline" class="password-field">
      <mat-label>{{ label() | translate }}</mat-label>
      <input
        matInput
        #inputRef
        [type]="visible() ? 'text' : 'password'"
        [attr.autocomplete]="autocomplete()"
        [attr.name]="name()"
        [required]="required()"
        [disabled]="disabled()"
        [attr.aria-label]="label() | translate"
        (input)="onInput($event)"
        (keyup)="checkCapsLock($event)"
        (keydown)="checkCapsLock($event)"
        (blur)="onTouched()"
      />
      <button
        matSuffix
        mat-icon-button
        type="button"
        (click)="toggle()"
        [attr.aria-label]="(visible() ? 'common.actions.hidePassword' : 'common.actions.showPassword') | translate"
        [attr.aria-pressed]="visible()"
      >
        <mat-icon aria-hidden="true">{{ visible() ? 'visibility_off' : 'visibility' }}</mat-icon>
      </button>
      @if (capsLockOn()) {
        <mat-hint align="start" class="password-field__caps">
          <mat-icon aria-hidden="true">keyboard_capslock</mat-icon>
          <span>{{ 'authentication.hints.capsLock' | translate }}</span>
        </mat-hint>
      }
      <ng-content select="mat-error"></ng-content>
      <ng-content select="mat-hint"></ng-content>
    </mat-form-field>
  `,
  styles: [
    `
      :host { display: block; }
      .password-field { width: 100%; }
      .password-field__caps {
        display: inline-flex;
        align-items: center;
        gap: var(--app-space-1);
        color: var(--app-color-warning);
      }
      .password-field__caps mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    `
  ]
})
export class PasswordInputComponent implements ControlValueAccessor {
  @ViewChild('inputRef', { static: true }) inputRef!: ElementRef<HTMLInputElement>;
  readonly label = input.required<string>();
  readonly required = input<boolean>(true);
  readonly autocomplete = input<string>('current-password');
  readonly name = input<string>('password');

  readonly visible = signal(false);
  readonly capsLockOn = signal(false);
  readonly disabled = signal(false);

  private onChange: (value: string) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(value: string | null): void {
    if (this.inputRef?.nativeElement) {
      this.inputRef.nativeElement.value = value ?? '';
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  toggle(): void {
    this.visible.update((v) => !v);
  }

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.onChange(target.value);
  }

  checkCapsLock(event: KeyboardEvent): void {
    if (typeof event.getModifierState === 'function') {
      this.capsLockOn.set(event.getModifierState('CapsLock'));
    }
  }
}
