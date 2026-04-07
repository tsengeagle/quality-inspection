package com.tzuchi.his.quality.inspection.interfaces.rest;

import java.lang.annotation.*;

/**
 * Marks a controller method parameter to be resolved from the ACTOR_CONTEXT request attribute.
 * The ApiKeyAuthFilter sets this attribute before the request reaches the controller.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentActor {
}
