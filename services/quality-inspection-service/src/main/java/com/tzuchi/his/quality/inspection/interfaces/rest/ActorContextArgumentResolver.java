package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.infrastructure.security.ApiKeyAuthFilter;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

/**
 * Resolves @CurrentActor ActorContext parameters from the ACTOR_CONTEXT request attribute
 * set by ApiKeyAuthFilter.
 */
@Component
public class ActorContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentActor.class)
                && ActorContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("No HttpServletRequest available");
        }
        Object actor = request.getAttribute(ApiKeyAuthFilter.ACTOR_CONTEXT_ATTR);
        if (actor == null) {
            throw new IllegalStateException("No ActorContext found in request — ApiKeyAuthFilter may not have run");
        }
        return actor;
    }
}
