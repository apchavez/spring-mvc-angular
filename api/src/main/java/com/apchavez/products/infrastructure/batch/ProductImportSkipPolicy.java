package com.apchavez.products.infrastructure.batch;

import com.apchavez.products.domain.exception.ProductDomainException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.retry.RetryException;

/**
 * Skips only row-level, data-quality failures — bad CSV syntax ({@link FlatFileParseException}),
 * unparsable numeric fields ({@link NumberFormatException}), and domain validation failures
 * ({@link ProductDomainException}, e.g. blank SKU or a duplicate one) — so one bad row never
 * aborts the whole import. Deliberately an allowlist, not "skip any Exception": an
 * infrastructure-level failure (e.g. the reader/writer itself breaking) must NOT be skipped,
 * since the row that triggered it is never actually retried — with an unconditional policy,
 * every subsequent read attempt keeps re-throwing the same non-recoverable error and the step
 * spins forever instead of failing fast (hit exactly this while building this module: an
 * unconditional "skip any Exception" policy turned a {@code ReaderNotOpenException} after the
 * reader had already closed into an infinite skip loop).
 * <p>
 * Spring Batch's fault-tolerant chunk processing always routes read/process/write through an
 * internal retry template (even with no {@code .retry(...)} configured), so the throwable
 * reaching this policy can arrive wrapped in a {@link RetryException} — unwrap it first.
 */
public class ProductImportSkipPolicy implements SkipPolicy {

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        Throwable actual = unwrap(t);
        return actual instanceof FlatFileParseException
                || actual instanceof NumberFormatException
                || actual instanceof ProductDomainException;
    }

    private Throwable unwrap(Throwable t) {
        if (t instanceof RetryException retryException) {
            return retryException.getCause();
        }
        return t;
    }
}
