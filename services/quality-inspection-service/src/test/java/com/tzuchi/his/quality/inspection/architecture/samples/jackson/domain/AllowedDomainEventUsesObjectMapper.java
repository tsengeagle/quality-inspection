package com.tzuchi.his.quality.inspection.architecture.samples.jackson.domain;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 合規對照：這條規則不管 domain。
 *
 * <p>domain 的事件用 ObjectMapper 產生稽核的 detail（D-080 第 5 點）。凍結它會把新事件推回拼字串，
 * 所以等「事件的 detail 改成結構化」做完再納入（D-096 第 2 點）。
 */
public class AllowedDomainEventUsesObjectMapper {
    private final ObjectMapper mapper = new ObjectMapper();

    int size() {
        return mapper.createObjectNode().size();
    }
}
