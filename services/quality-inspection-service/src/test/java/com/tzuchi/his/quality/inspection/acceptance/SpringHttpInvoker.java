package com.tzuchi.his.quality.inspection.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 以 MockMvc 送出 http 步驟的請求（D-074 第 8 點）。
 *
 * <ul>
 *   <li>請求在呼叫的執行緒上處理，所以與範例在同一個交易裡，事件也同步擷取。</li>
 *   <li>api-key 是沒有簽章的 JWT：程式不驗簽（閘道已驗），只讀 tz-user 的內容。</li>
 *   <li>路徑不含正式部署的前綴，直接打 controller 的路徑。</li>
 * </ul>
 */
public final class SpringHttpInvoker implements HttpInvoker {

    /** 程式用這個設定覆寫操作者的院區（TzJwtParser）。 */
    static final String HOSPITAL_OVERRIDE = "quality-inspection.hospital-id";

    private final MockMvc mockMvc;
    private final Environment environment;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    public SpringHttpInvoker(MockMvc mockMvc, Environment environment) {
        this.mockMvc = mockMvc;
        this.environment = environment;
    }

    @Override
    public void checkReady() {
        String override = environment.getProperty(HOSPITAL_OVERRIDE);
        if (override != null && !override.trim().isEmpty()) {
            throw new RunnerProblem("設定 " + HOSPITAL_OVERRIDE + " 有值（" + override.trim()
                    + "）：程式會忽略 api-key 的院區，http 範例的操作者會被換掉。清掉這個設定再執行（D-074 第 7 點）");
        }
    }

    @Override
    public Reply send(Request request) {
        HttpMethod method = HttpMethod.resolve(request.method);
        if (method == null) {
            throw new RunnerProblem("http.method 不是 HTTP 方法：" + request.method);
        }
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(method, request.path);
        for (Map.Entry<String, Object> entry : request.query.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Collection) {
                for (Object item : (Collection<?>) value) {
                    builder.queryParam(entry.getKey(), String.valueOf(item));
                }
            } else if (value != null) {
                builder.queryParam(entry.getKey(), String.valueOf(value));
            }
        }
        try {
            if (request.bodyRaw != null) {
                builder.contentType(MediaType.APPLICATION_JSON).content(request.bodyRaw.getBytes(StandardCharsets.UTF_8));
            } else if (request.body != null) {
                builder.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(request.body));
            }
            if (!request.anonymous) {
                builder.header("api-key", apiKey(request));
            }
        } catch (JsonProcessingException e) {
            throw new RunnerProblem("http 步驟的請求無法轉成 JSON：" + e.getMessage());
        }

        MockHttpServletResponse response;
        try {
            response = mockMvc.perform(builder).andReturn().getResponse();
        } catch (Exception e) {
            throw new RunnerProblem("http 步驟的請求沒有得到回應（" + request.method + " " + request.path + "）：" + e);
        }
        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        Object json = null;
        if (!body.trim().isEmpty()) {
            try {
                json = mapper.readValue(body, Object.class);
            } catch (JsonProcessingException e) {
                json = null;
            }
        }
        return new Reply(response.getStatus(), body, json);
    }

    /** 與閘道發的 JWT 同一個結構：payload 的 tz-user 是一段 JSON 字串，裡面的 user 帶 user_id、user_role、hospital_id。 */
    private String apiKey(Request request) throws JsonProcessingException {
        List<Map<String, Object>> roles = new ArrayList<>();
        for (String claim : request.roleClaims) {
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("role_name", claim);
            role.put("enabled", true);
            roles.add(role);
        }
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("user_id", request.user);
        user.put("user_name", request.user);
        user.put("user_role", roles);
        if (request.hospital != null) {
            user.put("hospital_id", request.hospital);
        }
        Map<String, Object> tzUser = new LinkedHashMap<>();
        tzUser.put("user", user);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS512");
        header.put("typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", "api-gateway");
        payload.put("tz-user", mapper.writeValueAsString(tzUser));
        return base64(mapper.writeValueAsBytes(header)) + "." + base64(mapper.writeValueAsBytes(payload)) + ".acceptance";
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
