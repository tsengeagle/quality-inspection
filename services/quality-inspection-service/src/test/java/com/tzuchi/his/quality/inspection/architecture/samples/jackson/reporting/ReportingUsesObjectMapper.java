package com.tzuchi.his.quality.inspection.architecture.samples.jackson.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 違規樣本：reporting 直接使用 ObjectMapper。 */
public class ReportingUsesObjectMapper {
    private final ObjectMapper mapper = new ObjectMapper();

    int size() {
        return mapper.createObjectNode().size();
    }
}
