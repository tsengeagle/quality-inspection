package com.tzuchi.his.quality.inspection.domain.scoresheet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 評分表版本 id（D-050 第 1 點）：由評分表 id 與版本號組成，不新增欄位（D-045）。
 * 對外是不透明的 id；只有 domain 會拆解它。
 */
class ScoreSheetVersionIdTest {

    @Test
    void formatAndParseRoundTrip() {
        String id = ScoreSheetVersionId.of("ss-123", 1);

        Optional<ScoreSheetVersionId> parsed = ScoreSheetVersionId.parse(id);

        assertTrue(parsed.isPresent());
        assertEquals("ss-123", parsed.get().getScoreSheetId());
        assertEquals(1, parsed.get().getVersionNo());
    }

    @Test
    void scoreSheetIdMayContainTheSeparatorCharacters() {
        String id = ScoreSheetVersionId.of("a:v2:b", 3);

        ScoreSheetVersionId parsed = ScoreSheetVersionId.parse(id).orElseThrow();

        assertEquals("a:v2:b", parsed.getScoreSheetId());
        assertEquals(3, parsed.getVersionNo());
    }

    @Test
    void malformedIdsAreNotVersionIds() {
        for (String bad : new String[] {null, "", "ss-123", "ss-123:v", ":v1", "ss-123:vx", "ss-123:v0", "ss-123:v-1",
                "ss-123:v99999999999", "ss-123:v1 ",
                "ss-123:v01", "ss-123:v000000001", "ss-123:v+1"}) {
            assertTrue(ScoreSheetVersionId.parse(bad).isEmpty(), "should not parse: " + bad);
        }
    }

    @Test
    void draftHasNoVersionIdUntilPublished() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1", ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);

        assertNull(ss.getVersionId());

        ss.publishVersion();

        assertEquals(ScoreSheetVersionId.of("ss1", 1), ss.getVersionId());
    }
}
