package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;

import java.util.*;

public class InMemoryScoreSheetRepository implements ScoreSheetRepository {
    public final Map<String, ScoreSheetDefinition> store = new LinkedHashMap<>();

    @Override
    public void save(ScoreSheetDefinition scoreSheet) {
        store.put(scoreSheet.getScoreSheetId(), scoreSheet);
    }

    @Override
    public Optional<ScoreSheetDefinition> findById(String scoreSheetId) {
        return Optional.ofNullable(store.get(scoreSheetId));
    }

    public void clear() { store.clear(); }
    public int size() { return store.size(); }
    public Collection<ScoreSheetDefinition> getAll() { return store.values(); }
}
