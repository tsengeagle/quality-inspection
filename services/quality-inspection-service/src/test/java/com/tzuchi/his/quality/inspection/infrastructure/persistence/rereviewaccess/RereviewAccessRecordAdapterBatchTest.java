package com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Oracle 的 IN 清單最多 1000 個值（ORA-01795）：分批且去重。 */
class RereviewAccessRecordAdapterBatchTest {

    @Test
    void splitsIntoBatchesOfAtMostOneThousand_andRemovesDuplicates() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 2500; i++) ids.add("c" + i);
        ids.add("c0");

        List<List<String>> batches = RereviewAccessRecordAdapter.batches(ids);

        assertThat(batches).extracting(List::size).containsExactly(1000, 1000, 500);
        assertThat(batches.stream().flatMap(List::stream).collect(Collectors.toSet())).hasSize(2500);
    }

    @Test
    void emptyInput_noBatches() {
        assertThat(RereviewAccessRecordAdapter.batches(List.of())).isEmpty();
    }
}
