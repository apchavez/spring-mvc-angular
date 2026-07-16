package com.apchavez.products.infrastructure.batch;

import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records one {@link RowError} per skipped row, keyed by jobExecutionId, so
 * {@code GET /api/v1/products/import/{jobExecutionId}} can report them after the async job
 * finishes. Row numbers: read-skips get the exact line number from
 * {@link FlatFileParseException}; process/write-skips use {@code currentRow}, a counter
 * incremented on every {@link #beforeRead()} call — safe because the step runs single-threaded
 * with chunk size 1, so it always reflects the row most recently pulled off the reader.
 */
public class ProductImportSkipListener implements SkipListener<ProductCsvRow, Object>, ItemReadListener<ProductCsvRow> {

    private static final Map<Long, List<RowError>> ERRORS_BY_JOB_EXECUTION = new ConcurrentHashMap<>();

    private final Long jobExecutionId;
    private int currentRow = 1; // row 1 is the header line itself

    public ProductImportSkipListener(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
        ERRORS_BY_JOB_EXECUTION.put(jobExecutionId, new CopyOnWriteArrayList<>());
    }

    @Override
    public void beforeRead() {
        currentRow++;
    }

    @Override
    public void afterRead(ProductCsvRow item) {
        // no-op — row tracking happens in beforeRead()
    }

    @Override
    public void onReadError(Exception ex) {
        // no-op — recorded via onSkipInRead() once the skip policy actually decides to skip
    }

    @Override
    public void onSkipInRead(Throwable t) {
        int row = (t instanceof FlatFileParseException ffpe) ? ffpe.getLineNumber() : currentRow;
        recordError(row, t);
    }

    @Override
    public void onSkipInProcess(ProductCsvRow item, Throwable t) {
        recordError(currentRow, t);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        recordError(currentRow, t);
    }

    private void recordError(int row, Throwable t) {
        String message = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        ERRORS_BY_JOB_EXECUTION.get(jobExecutionId).add(new RowError(row, message));
    }

    public static List<RowError> errorsFor(Long jobExecutionId) {
        return ERRORS_BY_JOB_EXECUTION.getOrDefault(jobExecutionId, List.of());
    }
}
