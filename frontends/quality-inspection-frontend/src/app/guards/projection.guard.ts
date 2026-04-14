import { Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class ProjectionGuard implements CanActivate {
  canActivate(): boolean {
    return true; // passthrough — formal metadata-only projection logic implemented post-Wave 1
  }
}
