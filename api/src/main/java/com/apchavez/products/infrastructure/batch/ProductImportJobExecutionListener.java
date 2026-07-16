package com.apchavez.products.infrastructure.batch;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Deletes the uploaded CSV's temp copy once the job finishes, regardless of outcome. */
public class ProductImportJobExecutionListener implements JobExecutionListener {

    @Override
    public void afterJob(JobExecution jobExecution) {
        String filePath = jobExecution.getJobParameters().getString("filePath");
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ignored) {
            // best-effort cleanup; the OS temp dir will eventually reclaim it
        }
    }
}
