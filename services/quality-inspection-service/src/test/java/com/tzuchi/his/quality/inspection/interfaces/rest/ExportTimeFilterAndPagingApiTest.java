package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase.ReviewCaseJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** D-057：匯出依案件建立時間過濾（含起不含迄）、from/to 必填、逐案資料列分頁；儀表板的時間規則。 */
@SpringBootTest
@AutoConfigureMockMvc
class ExportTimeFilterAndPagingApiTest {

    private static final Instant FROM = Instant.parse("2026-04-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-05-01T00:00:00Z");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ReviewCaseJpaRepository cases;

    private final String manager = TestTokenFactory.hospitalManagerToken();

    @BeforeEach
    void seed() {
        cases.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        cases.deleteAll();
    }

    private void save(String id, Instant createdAt) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(id); e.setBatchId("b"); e.setProjectId("p"); e.setHospitalId("H001");
        e.setStatus(CaseStatus.CLOSED);
        e.setCreatedAt(createdAt);
        cases.save(e);
    }

    private org.springframework.test.web.servlet.ResultActions export(String body) throws Exception {
        return mvc.perform(post("/reports/export").header("api-key", manager)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode exportOk(String body) throws Exception {
        String json = export(body).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).path("data");
    }

    private static String range(String extra) {
        return "{\"reportType\":\"REVIEW_COMPLETION\",\"from\":\"" + FROM + "\",\"to\":\"" + TO + "\"" + extra + "}";
    }

    private static List<String> caseIds(JsonNode data) {
        List<String> ids = new ArrayList<>();
        data.path("rows").forEach(r -> ids.add(r.path("caseId").asText()));
        return ids;
    }

    // ---- 第 2 點：from、to 必填，from 必須早於 to ----

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"reportType\":\"REVIEW_COMPLETION\"}",
            "{\"reportType\":\"REVIEW_COMPLETION\",\"from\":\"2026-04-01T00:00:00Z\"}",
            "{\"reportType\":\"REVIEW_COMPLETION\",\"to\":\"2026-05-01T00:00:00Z\"}",
            "{\"reportType\":\"REVIEW_COMPLETION\",\"from\":\"2026-05-01T00:00:00Z\",\"to\":\"2026-05-01T00:00:00Z\"}",
            "{\"reportType\":\"REVIEW_COMPLETION\",\"from\":\"2026-05-01T00:00:00Z\",\"to\":\"2026-04-01T00:00:00Z\"}"})
    void missingOrInvalidRange_isBadRequest(String body) throws Exception {
        export(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    // ---- 第 1、2 點：依案件建立時間，含起不含迄；NULL 不落在任何區間 ----

    @Test
    void filtersByCaseCreatedAt_fromInclusive_toExclusive_nullExcluded() throws Exception {
        save("a-before", FROM.minusSeconds(1));
        save("b-at-from", FROM);
        save("c-inside", FROM.plusSeconds(3600));
        save("d-at-to", TO);
        save("e-legacy-null", null);

        JsonNode data = exportOk(range(""));

        assertThat(caseIds(data)).containsExactly("b-at-from", "c-inside");
        assertThat(data.path("totalElements").asLong()).isEqualTo(2);
        assertThat(data.path("summary").path("totalCases").asLong()).as("summary 套用同一個區間").isEqualTo(2);
        assertThat(data.path("rows").get(0).path("createdAt").asText()).isEqualTo(FROM.toString());
    }

    // ---- 第 3 點：分頁；summary 與 totalElements 以整個過濾範圍計算 ----

    @Test
    void pagesAreOrderedByCaseId_andSummaryCoversTheWholeRange() throws Exception {
        for (String id : List.of("c5", "c1", "c4", "c2", "c3")) save(id, FROM.plusSeconds(60));

        JsonNode data = exportOk(range(",\"page\":1,\"size\":2"));

        assertThat(caseIds(data)).containsExactly("c3", "c4");
        assertThat(data.path("page").asInt()).isEqualTo(1);
        assertThat(data.path("size").asInt()).isEqualTo(2);
        assertThat(data.path("totalElements").asLong()).isEqualTo(5);
        assertThat(data.path("totalPages").asInt()).isEqualTo(3);
        assertThat(data.path("summary").path("totalCases").asLong()).isEqualTo(5);
    }

    @Test
    void pagingDefaultsAndNormalization() throws Exception {
        save("c1", FROM.plusSeconds(60));

        JsonNode defaults = exportOk(range(""));
        assertThat(defaults.path("page").asInt()).isEqualTo(0);
        assertThat(defaults.path("size").asInt()).isEqualTo(100);

        JsonNode nonPositive = exportOk(range(",\"page\":-3,\"size\":0"));
        assertThat(nonPositive.path("page").asInt()).isEqualTo(0);
        assertThat(nonPositive.path("size").asInt()).isEqualTo(100);

        JsonNode tooLarge = exportOk(range(",\"size\":5000"));
        assertThat(tooLarge.path("size").asInt()).isEqualTo(1000);
    }

    @Test
    void moreThanTenThousandCasesAreNoLongerTruncated() throws Exception {
        // 原本 setMaxResults(10000) 會靜默截斷；改為分頁後，totalElements 反映整個範圍。
        List<ReviewCaseEntity> bulk = new ArrayList<>();
        for (int i = 0; i < 10_050; i++) {
            ReviewCaseEntity e = new ReviewCaseEntity();
            e.setCaseId(String.format("k%05d", i)); e.setBatchId("b"); e.setProjectId("p"); e.setHospitalId("H001");
            e.setStatus(CaseStatus.CLOSED); e.setCreatedAt(FROM.plusSeconds(i));
            bulk.add(e);
        }
        cases.saveAll(bulk);

        JsonNode lastPage = exportOk(range(",\"page\":10,\"size\":1000"));

        assertThat(lastPage.path("totalElements").asLong()).isEqualTo(10_050);
        assertThat(lastPage.path("totalPages").asInt()).isEqualTo(11);
        assertThat(caseIds(lastPage)).hasSize(50).first().isEqualTo("k10000");
    }

    // ---- 第 4 點：儀表板 from、to 選填；兩個都給才過濾 ----

    @Test
    void dashboard_bothGiven_filtersByCreatedAt_noneGiven_countsAll() throws Exception {
        save("in", FROM.plusSeconds(60));
        save("out", TO.plusSeconds(60));
        save("legacy", null);

        mvc.perform(get("/reports/dashboard-summary").header("api-key", manager)
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCases").value(1));
        mvc.perform(get("/reports/dashboard-summary").header("api-key", manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCases").value(3));
    }

    /** 院區、專案與時間區間同時有條件時，JPQL 的位置參數（?1～?4）不得錯位。 */
    @Test
    void dashboard_hospitalProjectAndRangeTogether() throws Exception {
        save("in", FROM.plusSeconds(60));
        ReviewCaseEntity other = new ReviewCaseEntity();
        other.setCaseId("other-project"); other.setBatchId("b"); other.setProjectId("p2"); other.setHospitalId("H001");
        other.setStatus(CaseStatus.CLOSED); other.setCreatedAt(FROM.plusSeconds(60));
        cases.save(other);

        mvc.perform(get("/reports/dashboard-summary").header("api-key", manager).param("projectId", "p"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCases").value(1));
        mvc.perform(get("/reports/dashboard-summary").header("api-key", manager).param("projectId", "p")
                        .param("from", FROM.toString()).param("to", TO.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCases").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"from-only", "to-only", "equal", "reversed"})
    void dashboard_invalidRange_isBadRequest(String scenario) throws Exception {
        var req = get("/reports/dashboard-summary").header("api-key", manager);
        switch (scenario) {
            case "from-only": req = req.param("from", FROM.toString()); break;
            case "to-only": req = req.param("to", TO.toString()); break;
            case "equal": req = req.param("from", TO.toString()).param("to", TO.toString()); break;
            default: req = req.param("from", TO.toString()).param("to", FROM.toString());
        }
        mvc.perform(req).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }
}
