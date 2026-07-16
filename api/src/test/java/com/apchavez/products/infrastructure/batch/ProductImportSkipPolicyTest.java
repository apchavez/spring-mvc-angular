package com.apchavez.products.infrastructure.batch;

import com.apchavez.products.domain.exception.InvalidProductException;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.retry.RetryException;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImportSkipPolicyTest {

    private final ProductImportSkipPolicy policy = new ProductImportSkipPolicy();

    @Test
    void shouldSkip_shouldReturnTrue_forAFlatFileParseException() {
        assertThat(policy.shouldSkip(new FlatFileParseException("bad row", "raw,line", 3), 0)).isTrue();
    }

    @Test
    void shouldSkip_shouldReturnTrue_forANumberFormatException() {
        assertThat(policy.shouldSkip(new NumberFormatException("not a number"), 0)).isTrue();
    }

    @Test
    void shouldSkip_shouldReturnTrue_forADomainValidationException() {
        assertThat(policy.shouldSkip(new InvalidProductException("El SKU no puede estar vacío"), 0)).isTrue();
    }

    @Test
    void shouldSkip_shouldReturnFalse_forAnUnrelatedInfrastructureException() {
        // Anything outside the allowlist must fail the step instead of being skipped — an
        // unconditional "skip any Exception" policy previously turned an unrelated,
        // non-recoverable reader failure into an infinite skip loop (see class javadoc).
        assertThat(policy.shouldSkip(new IllegalStateException("reader is broken"), 0)).isFalse();
    }

    @Test
    void shouldSkip_shouldReturnFalse_forAnError() {
        assertThat(policy.shouldSkip(new OutOfMemoryError(), 0)).isFalse();
    }

    @Test
    void shouldSkip_shouldUnwrapRetryException_andDecideOnTheRealCause() {
        RetryException wrapped = new RetryException("retry failed", new NumberFormatException("real cause"));

        assertThat(policy.shouldSkip(wrapped, 0)).isTrue();
    }
}
