package com.tzuchi.his.quality.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates unsigned test JWTs for @SpringBootTest integration tests.
 * The service does not re-verify the JWT signature (gateway already did),
 * so a fake signature is accepted.
 */
public class TestTokenFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String DEFAULT_HOSPITAL_ID = "H001";

    /** Returns a JWT with HOSPITAL_MANAGER role and default hospitalId (H001). */
    public static String hospitalManagerToken() {
        return hospitalManagerToken(DEFAULT_HOSPITAL_ID);
    }

    /** Returns a JWT with HOSPITAL_MANAGER role and specified hospitalId. */
    public static String hospitalManagerToken(String hospitalId) {
        return buildToken("u-test-01", "Test Manager", hospitalId,
                Map.of("role_name", "hospital_manager", "enabled", true));
    }

    /** Returns a JWT with SYSTEM_ADMIN role and specified hospitalId. */
    public static String systemAdminToken(String hospitalId) {
        return buildToken("u-admin-01", "Test Admin", hospitalId,
                Map.of("role_name", "system_admin", "enabled", true));
    }

    /** Returns a JWT with SYSTEM_ADMIN role and default hospitalId (H001). */
    public static String systemAdminToken() {
        return systemAdminToken(DEFAULT_HOSPITAL_ID);
    }

    /** Returns a JWT with an unknown role (no DomainRole match). */
    public static String unknownRoleToken() {
        return buildToken("u-unknown", "Unknown Role User", null,
                Map.of("role_name", "unknown_role", "enabled", true));
    }

    /** Returns a JWT with REVIEWER role and specified userId + hospitalId. */
    public static String reviewerToken(String userId, String hospitalId) {
        return buildToken(userId, "Test Reviewer", hospitalId,
                Map.of("role_name", "reviewer", "enabled", true));
    }

    /** Returns a JWT with REVIEWER role, default userId and hospitalId. */
    public static String reviewerToken() {
        return reviewerToken("reviewer-1", DEFAULT_HOSPITAL_ID);
    }

    private static String buildToken(String userId, String userName,
                                     String hospitalId, Map<String, Object> role) {
        try {
            String header = base64url(MAPPER.writeValueAsString(
                    Map.of("alg", "RS512", "typ", "JWT")));

            // Build user object — use LinkedHashMap to allow null values
            Map<String, Object> userObj = new LinkedHashMap<>();
            userObj.put("user_id", userId);
            userObj.put("user_name", userName);
            userObj.put("user_role", new Object[]{role});
            if (hospitalId != null) {
                userObj.put("hospital_id", hospitalId);
            }
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
