import { Directive, Input, OnDestroy, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ActorContextService } from '@core/actor-context/actor-context.service';

import { AccessCheck } from './access-control.model';
import { AccessControlService } from './access-control.service';

@Directive({
  selector: '[appCanAccess]'
})
export class CanAccessDirective implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private hasView = false;
  private accessCheck: AccessCheck | null = null;

  @Input()
  set appCanAccess(value: AccessCheck | null | undefined) {
    this.accessCheck = value ?? null;
    this.render();
  }

  constructor(
    private readonly templateRef: TemplateRef<unknown>,
    private readonly viewContainer: ViewContainerRef,
    private readonly actorContextService: ActorContextService,
    private readonly accessControlService: AccessControlService
  ) {}

  ngOnInit(): void {
    this.actorContextService.context$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.render());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private render(): void {
    if (!this.accessCheck) {
      this.clearView();
      return;
    }

    const canRender = this.accessControlService.canAccess(this.accessCheck);

    if (canRender && !this.hasView) {
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
      return;
    }

    if (!canRender) {
      this.clearView();
    }
  }

  private clearView(): void {
    if (!this.hasView) {
      return;
    }

    this.viewContainer.clear();
    this.hasView = false;
  }
}