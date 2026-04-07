package com.tzuchi.his.quality.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;

/**
 * Generates unsigned test JWTs for @SpringBootTest integration tests.
 * The service does not re-verify the JWT signature (gateway already did),
 * so a fake signature is accepted.
 */
public class TestTokenFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Returns a JWT with a HOSPITAL_MANAGER role for test user u-test-01. */
    public static String hospitalManagerToken() {
        return buildToken("u-test-01", "Test Manager",
                Map.of("role_name", "hospital_manager", "enabled", true));
    }

    /** Returns a JWT with an unknown role (no DomainRole match). */
    public static String unknownRoleToken() {
        return buildToken("u-unknown", "Unknown Role User",
                Map.of("role_name", "unknown_role", "enabled", true));
    }

    private static String buildToken(String userId, String userName, Map<String, Object> role) {
        try {
            String header = base64url(MAPPER.writeValueAsString(Map.of("alg", "RS512", "typ", "JWT")));

            // tz-user is double-encoded: it's a JSON string within the payload
            Map<String, Object> userObj = Map.of(
                    "user_id", userId,
                    "user_name", userName,
                    "user_role", new Object[]{role}
            );
            String tzUserJson = MAPPER.writeValueAsString(Map.of("user", userObj));

            String payloadJson = MAPPER.writeValueAsString(Map.of(
                    "iss", "api-gateway",
                    "tz-user", tzUserJson
            ));
            String payload = base64url(payloadJson);

            return header + "." + payload + ".test-sig";
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test token", e);
        }
    }

    private static String base64url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
