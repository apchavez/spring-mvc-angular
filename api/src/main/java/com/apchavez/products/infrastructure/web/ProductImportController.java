package com.apchavez.products.infrastructure.web;

import com.apchavez.products.infrastructure.batch.ProductImportSkipListener;
import com.apchavez.products.infrastructure.web.dto.ImportJobResponseDTO;
import com.apchavez.products.infrastructure.web.dto.ImportJobStatusResponseDTO;
import com.apchavez.products.infrastructure.web.dto.ImportRowErrorDTO;
import com.apchavez.products.infrastructure.web.exception.InvalidImportFileException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

/**
 * Async CSV bulk-import via Spring Batch — mirrors spring-webflux-angular's contract exactly
 * (202 + jobExecutionId, then poll GET .../{jobExecutionId}) so the two Spring siblings behave
 * identically from the client's point of view, only the runtime underneath differs.
 */
@RestController
@RequestMapping("/api/v1/products/import")
@Tag(name = "Product Import", description = "Importación masiva de productos vía CSV (Spring Batch, asíncrono)")
public class ProductImportController {

    private final JobOperator productImportJobOperator;
    private final Job productImportJob;
    private final JobExplorer jobExplorer;

    public ProductImportController(JobOperator productImportJobOperator, Job productImportJob, JobExplorer jobExplorer) {
        this.productImportJobOperator = productImportJobOperator;
        this.productImportJob = productImportJob;
        this.jobExplorer = jobExplorer;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importar productos desde CSV (asíncrono)",
            description = "Sube un archivo CSV (columnas: sku,name,description,category,price,stock,active) y " +
                    "lanza un Job de Spring Batch en segundo plano. Devuelve 202 con el jobExecutionId para hacer " +
                    "polling del resultado en GET /api/v1/products/import/{jobExecutionId}. Las filas inválidas se " +
                    "omiten y se reportan, no abortan el import completo. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Job de import aceptado",
                    content = @Content(schema = @Schema(implementation = ImportJobResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Archivo vacío o el Job no pudo lanzarse")
    })
    public ResponseEntity<ImportJobResponseDTO> importProducts(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidImportFileException("El archivo CSV no puede estar vacío");
        }

        Path tempFile = createSecureTempFile();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        var jobParameters = new JobParametersBuilder()
                .addString("filePath", tempFile.toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution;
        try {
            execution = productImportJobOperator.start(productImportJob, jobParameters);
        } catch (Exception ex) {
            Files.deleteIfExists(tempFile);
            throw new InvalidImportFileException("No se pudo iniciar el import: " + ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ImportJobResponseDTO(execution.getId(), execution.getStatus().toString()));
    }

    @GetMapping("/{jobExecutionId}")
    @Operation(summary = "Consultar estado de un import",
            description = "Devuelve el estado del Job y, si ya avanzó, el detalle de filas importadas/omitidas hasta el momento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del job",
                    content = @Content(schema = @Schema(implementation = ImportJobStatusResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "No existe un import con ese ID")
    })
    public ResponseEntity<ImportJobStatusResponseDTO> getImportStatus(@PathVariable Long jobExecutionId) {
        JobExecution execution = jobExplorer.getJobExecution(jobExecutionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        List<ImportRowErrorDTO> errors = ProductImportSkipListener.errorsFor(jobExecutionId).stream()
                .map(e -> new ImportRowErrorDTO(e.row(), e.message()))
                .toList();

        int imported = (int) execution.getStepExecutions().stream()
                .mapToLong(step -> step.getWriteCount())
                .sum();

        return ResponseEntity.ok(new ImportJobStatusResponseDTO(
                execution.getId(),
                execution.getStatus().toString(),
                imported,
                errors.size(),
                errors));
    }

    private Path createSecureTempFile() throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return Files.createTempFile("product-import-", ".csv",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        }
        return Files.createTempFile("product-import-", ".csv");
    }
}
