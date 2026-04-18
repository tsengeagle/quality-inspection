import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

import { ACTOR_CONTEXT_FOUNDATION_PROVIDERS } from './actor-context/actor-context.providers';

/**
 * CoreModule — singleton services, interceptors, guards.
 * Imported once in AppModule.
 */
@NgModule({
  imports: [
    CommonModule,
    HttpClientModule
  ],
  providers: [ACTOR_CONTEXT_FOUNDATION_PROVIDERS]
})
export class CoreModule { }
