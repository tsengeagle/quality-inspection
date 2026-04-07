package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TzJwtParserHospitalIdTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parse_withJwtHospitalId_shouldExtract() {
        TzJwtParser parser = new TzJwtParser();
        // no env var configured
        String token = buildTokenWithHospitalId("H-JWT-001");

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-JWT-001", actor.getHospitalId().get());
    }

    @Test
    void parse_withoutHospitalId_shouldReturnEmpty() {
        TzJwtParser parser = new TzJwtParser();
        String token = buildTokenWithoutHospitalId();

        ActorContext actor = parser.parse(token);

        assertFalse(actor.getHospitalId().isPresent());
    }

    @Test
    void parse_withEnvHospitalId_shouldOverrideJwt() {
        TzJwtParser parser = new TzJwtParser();
        parser.setConfiguredHospitalId("H-ENV-001");
        String token = buildTokenWithHospitalId("H-JWT-001");

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-ENV-001", actor.getHospitalId().get());
    }

    @Test
    void parse_withEnvHospitalId_noJwtHospitalId_shouldUseEnv() {
        TzJwtParser parser = new TzJwtParser();
        parser.setConfiguredHospitalId("H-ENV-002");
        String token = buildTokenWithoutHospitalId();

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-ENV-002", actor.getHospitalId().get());
    }

    @Test
    void parse_withBlankEnvHospitalId_shouldFallbackToJwt() {
        TzJwtParser parser = new TzJwtParser();
        parser.setConfiguredHospitalId("  ");
        String token = buildTokenWithHospitalId("H-JWT-002");

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-JWT-002", actor.getHospitalId().get());
    }

    private String buildTokenWithHospitalId(String hospitalId) {
        try {
            String header = base64url(mapper.writeValueAsString(
                    Map.of("alg", "RS512", "typ", "JWT")));
            Map<String, Object> userObj = Map.of(
                    "user_id", "u-test",
                    "user_name", "Test User",
                    "user_role", new Object[]{Map.of("role_name", "hospital_manager", "enabled", true)},
                    "hospital_id", hospitalId
            );
            String tzUserJson = mapper.writeValueAsString(Map.of("user", userObj));
            String payload = base64url(mapper.writeValueAsString(
                    Map.of("iss", "api-gateway", "tz-user", tzUserJson)));
            return header + "." + payload + ".test-sig";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildTokenWithoutHospitalId() {
        try {
            String header = base64url(mapper.writeValueAsString(
                    Map.of("alg", "RS512", "typ", "JWT")));
            Map<String, Object> userObj = Map.of(
                    "user_id", "u-test",
                    "user_name", "Test User",
                    "user_role", new Object[]{Map.of("role_name", "hospital_manager", "enabled", true)}
            );
            String tzUserJson = mapper.writeValueAsString(Map.of("user", userObj));
            String payload = base64url(mapper.writeValueAsString(
                    Map.of("iss", "api-gateway", "tz-user", tzUserJson)));
            return header + "." + payload + ".test-sig";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String base64url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
