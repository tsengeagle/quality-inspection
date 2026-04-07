package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

/**
 * Parses the api-key JWT header to extract ActorContext.
 * <p>
 * The gateway already verified the signature (RS512, issuer=api-gateway).
 * This parser does NOT re-verify the signature — it only decodes and extracts claims.
 * <p>
 * JWT structure:
 *   header.payload.signature (Base64URL-encoded)
 * Payload contains a "tz-user" claim which is a JSON-encoded string (double-encoded).
 * After decoding: {"user":{"user_id":"...","user_name":"...","user_role":[{"role_name":"hospital_manager","enabled":true}]}}
 */
@Component
public class TzJwtParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Parse the JWT token string and return an ActorContext.
     *
     * @param jwtToken the raw JWT token from the api-key header
     * @return ActorContext with userId, userName, and roles
     * @throws JwtParseException if the token is malformed or missing required claims
     */
    public ActorContext parse(String jwtToken) {
        if (jwtToken == null || jwtToken.isBlank()) {
            throw new JwtParseException("api-key header is missing or blank");
        }

        String[] parts = jwtToken.split("\\.");
        if (parts.length < 2) {
            throw new JwtParseException("Invalid JWT structure: expected at least 2 parts");
        }

        String payloadJson;
        try {
            payloadJson = new String(Base64.getUrlDecoder().decode(addPadding(parts[1])));
        } catch (Exception e) {
            throw new JwtParseException("Failed to base64url-decode JWT payload", e);
        }

        JsonNode payload;
        try {
            payload = MAPPER.readTree(payloadJson);
        } catch (Exception e) {
            throw new JwtParseException("Failed to parse JWT payload JSON", e);
        }

        JsonNode tzUserNode = payload.get("tz-user");
        if (tzUserNode == null) {
            throw new JwtParseException("JWT payload missing required claim: tz-user");
        }

        // tz-user is a JSON string (double-encoded) — must parse again
        String tzUserJson = tzUserNode.asText();
        ParsedJwtUser parsedUser;
        try {
            parsedUser = MAPPER.readValue(tzUserJson, ParsedJwtUser.class);
        } catch (Exception e) {
            throw new JwtParseException("Failed to parse tz-user claim JSON", e);
        }

        if (parsedUser.getUser() == null) {
            throw new JwtParseException("tz-user claim missing 'user' field");
        }

        String userId = parsedUser.getUser().getUser_id();
        String userName = parsedUser.getUser().getUser_name();

        Set<DomainRole> roles = EnumSet.noneOf(DomainRole.class);
        if (parsedUser.getUser().getUser_role() != null) {
            for (ParsedJwtUser.UserRole userRole : parsedUser.getUser().getUser_role()) {
                if (userRole.isEnabled() && userRole.getRole_name() != null) {
                    try {
                        DomainRole role = DomainRole.valueOf(userRole.getRole_name().toUpperCase());
                        roles.add(role);
                    } catch (IllegalArgumentException ignored) {
                        // Unknown role — skip silently
                    }
                }
            }
        }

        return new ActorContext(userId, userName, roles);
    }

    private String addPadding(String base64url) {
        int remainder = base64url.length() % 4;
        if (remainder == 0) return base64url;
        StringBuilder sb = new StringBuilder(base64url);
        for (int i = 0; i < 4 - remainder; i++) {
            sb.append('=');
        }
        return sb.toString();
    }
}
