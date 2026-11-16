package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

/** D-045 第 3 點：樂觀鎖失敗回 409 CONFLICT（沿用既有通用代碼）。 */
class GlobalExceptionHandlerOptimisticLockTest {

    @Test
    void optimisticLockFailureIs409Conflict() {
        ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionHandler().handleOptimisticLock(
                new ObjectOptimisticLockingFailureException("ReviewCaseEntity", "c1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError().getCode()).isEqualTo("CONFLICT");
    }
}
