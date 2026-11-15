package com.tzuchi.his.quality.inspection.application.common;

/**
 * Port for generating identifiers of new aggregates.
 * <p>
 * The application layer must not call UUID.randomUUID() directly (architecture rule ARCH-02):
 * a generator behind a port can be replaced in tests to give stable ids.
 */
public interface IdGenerator {

    /** Returns a new identifier, unique across all aggregates. */
    String newId();
}
