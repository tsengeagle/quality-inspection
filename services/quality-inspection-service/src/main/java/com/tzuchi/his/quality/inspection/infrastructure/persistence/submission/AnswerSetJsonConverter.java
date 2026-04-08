package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Converter
public class AnswerSetJsonConverter implements AttributeConverter<List<AnswerItem>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<AnswerItem> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            List<Map<String, Object>> list = attribute.stream().map(a -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("itemId", a.getItemId());
                map.put("answerValue", a.getAnswerValue());
                map.put("derivedScore", a.getDerivedScore() != null ? a.getDerivedScore().toPlainString() : null);
                return map;
            }).collect(Collectors.toList());
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize answerSet to JSON", e);
        }
    }

    @Override
    public List<AnswerItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData)) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> list = MAPPER.readValue(dbData,
                    new TypeReference<List<Map<String, Object>>>() {});
            return list.stream().map(m -> {
                String itemId = (String) m.get("itemId");
                if (itemId == null) {
                    throw new IllegalStateException("Invalid JSON: itemId must not be null");
                }
                String answerValue = (String) m.get("answerValue");
                if (answerValue == null) {
                    throw new IllegalStateException("Invalid JSON: answerValue must not be null for itemId: " + itemId);
                }
                BigDecimal derivedScore = m.get("derivedScore") != null
                        ? new BigDecimal(m.get("derivedScore").toString())
                        : null;
                return AnswerItem.reconstitute(itemId, answerValue, derivedScore);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize answerSet from JSON", e);
        }
    }
}
