package com.tzuchi.his.quality.inspection.interfaces.config;

import com.tzuchi.his.quality.inspection.interfaces.rest.ActorContextArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ActorContextArgumentResolver actorContextArgumentResolver;

    public WebMvcConfig(ActorContextArgumentResolver actorContextArgumentResolver) {
        this.actorContextArgumentResolver = actorContextArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(actorContextArgumentResolver);
    }
}
