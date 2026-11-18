package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.infrastructure.id.UuidIdGenerator;

/**
 * 及格線第 1 項的驗證用違規（04-definition-of-enough.md）：interfaces 直接使用 infrastructure。
 * 這個 PR 不會合併；CI 必須因 ARCH-01／ARCH-04 失敗。
 */
class GateProbeLayerViolation {
    String nextId() {
        return new UuidIdGenerator().newId();
    }
}
