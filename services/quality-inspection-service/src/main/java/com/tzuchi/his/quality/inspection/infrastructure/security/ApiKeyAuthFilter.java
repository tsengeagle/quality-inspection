package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Reads the api-key JWT header, parses it via TzJwtParser, and stores the ActorContext
 * as a request attribute. If the header is missing or invalid, returns 401.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String ACTOR_CONTEXT_ATTR = "ACTOR_CONTEXT";

    private final TzJwtParser jwtParser;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(TzJwtParser jwtParser, ObjectMapper objectMapper) {
        this.jwtParser = jwtParser;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("api-key");

        if (apiKey == null || apiKey.isBlank()) {
            writeUnauthorized(response, "Missing api-key header");
            return;
        }

        try {
            ActorContext actor = jwtParser.parse(apiKey);
            request.setAttribute(ACTOR_CONTEXT_ATTR, actor);
        } catch (JwtParseException e) {
            writeUnauthorized(response, "Invalid api-key: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error("UNAUTHORIZED", message)));
    }
}
