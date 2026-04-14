import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class ScopeGuard implements CanActivate {
  canActivate(): boolean {
    return true; // passthrough — formal hospital-scope logic implemented post-Wave 1
  }
}
