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

    /** 未認證一律是 401 與 UNAUTHORIZED（D-023、api-spec §4.1）；缺少與無效的差別只寫在 message。 */
    private static final String UNAUTHORIZED = "UNAUTHORIZED";

    public ApiKeyAuthFilter(TzJwtParser jwtParser, ObjectMapper objectMapper) {
        this.jwtParser = jwtParser;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("api-key");

        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, UNAUTHORIZED, "Missing api-key header");
            return;
        }

        try {
            ActorContext actor = jwtParser.parse(apiKey);
            request.setAttribute(ACTOR_CONTEXT_ATTR, actor);
        } catch (JwtParseException e) {
            writeError(response, UNAUTHORIZED, "Invalid api-key: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
