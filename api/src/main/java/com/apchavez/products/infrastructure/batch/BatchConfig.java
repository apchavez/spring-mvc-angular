package com.apchavez.products.infrastructure.batch;

import com.apchavez.products.application.ProductApplicationService;
import com.apchavez.products.domain.model.Product;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch wiring for the CSV bulk-import job. Deliberately does NOT use
 * {@code @EnableBatchProcessing} (that would take over {@code JobRepository} configuration and
 * disable Boot's own batch autoconfiguration). Unlike the WebFlux sibling — which is reactive
 * (R2DBC) and has to build its own ad-hoc blocking {@code DataSource} just for this job, since
 * Boot's {@code DataSourceAutoConfiguration} never fires there — this app already uses Spring
 * Data JDBC natively, so Boot already publishes one real {@code PlatformTransactionManager}
 * bean; it's injected directly into the step below instead of building a second, parallel one
 * (which would make {@code @Transactional}'s default bean lookup ambiguous elsewhere in the app).
 */
@Configuration
public class BatchConfig {

    @Bean
    public JobRegistry productImportJobRegistry() {
        return new MapJobRegistry();
    }

    @Bean
    public JobOperator productImportJobOperator(JobRepository jobRepository, JobRegistry jobRegistry) throws Exception {
        TaskExecutorJobOperator operator = new TaskExecutorJobOperator();
        operator.setJobRepository(jobRepository);
        operator.setJobRegistry(jobRegistry);
        operator.setTaskExecutor(new SimpleAsyncTaskExecutor("product-import-"));
        operator.afterPropertiesSet();
        return operator;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ProductCsvRow> productCsvItemReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        // Concrete return type matters: a @StepScope proxy is built from this method's declared
        // return type, and FlatFileItemReader also implements ItemStream (needed so the step
        // opens/closes it automatically). Declaring ItemReader<T> here instead built a proxy
        // that DIDN'T expose ItemStream, so the reader was never opened before the first read()
        // — surfaced as a hard-to-place ReaderNotOpenException on every single row.
        return new FlatFileItemReaderBuilder<ProductCsvRow>()
                .name("productCsvItemReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .delimited()
                .names("sku", "name", "description", "category", "price", "stock", "active")
                .fieldSetMapper(new ProductCsvFieldSetMapper())
                .strict(true)
                .build();
    }

    @Bean
    public ItemProcessor<ProductCsvRow, Product> productImportItemProcessor() {
        return new ProductImportItemProcessor();
    }

    @Bean
    public ItemWriter<Product> productImportItemWriter(ProductApplicationService applicationService) {
        return new ProductImportItemWriter(applicationService);
    }

    @Bean
    @StepScope
    public ProductImportSkipListener productImportSkipListener(
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new ProductImportSkipListener(jobExecutionId);
    }

    @Bean
    public ProductImportJobExecutionListener productImportJobExecutionListener() {
        return new ProductImportJobExecutionListener();
    }

    @Bean
    public Step productImportStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   ItemReader<ProductCsvRow> productCsvItemReader,
                                   ItemProcessor<ProductCsvRow, Product> productImportItemProcessor,
                                   ItemWriter<Product> productImportItemWriter,
                                   ProductImportSkipListener productImportSkipListener) {
        return new StepBuilder("productImportStep", jobRepository)
                .<ProductCsvRow, Product>chunk(1, transactionManager)
                .reader(productCsvItemReader)
                .processor(productImportItemProcessor)
                .writer(productImportItemWriter)
                .faultTolerant()
                .skipPolicy(new ProductImportSkipPolicy())
                .listener((SkipListener<ProductCsvRow, Object>) productImportSkipListener)
                .listener((ItemReadListener<ProductCsvRow>) productImportSkipListener)
                .build();
    }

    @Bean
    public Job productImportJob(JobRepository jobRepository,
                                 Step productImportStep,
                                 ProductImportJobExecutionListener productImportJobExecutionListener) {
        return new JobBuilder("productImportJob", jobRepository)
                .start(productImportStep)
                .listener(productImportJobExecutionListener)
                .build();
    }
}
