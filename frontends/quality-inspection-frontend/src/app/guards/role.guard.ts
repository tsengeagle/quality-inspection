import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  canActivate(): boolean {
    return true; // passthrough — formal role-based logic implemented post-Wave 1
  }
}
