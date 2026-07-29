package com.apchavez.products.infrastructure.web;

import com.apchavez.products.AbstractIntegrationTest;
import com.apchavez.products.infrastructure.config.JwtService;
import com.apchavez.products.infrastructure.persistence.ProductEntity;
import com.apchavez.products.infrastructure.persistence.ProductJdbcRepository;
import com.apchavez.products.infrastructure.web.dto.ProductRequestDTO;
import com.apchavez.products.infrastructure.web.dto.ProductUpdateRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ProductControllerIntegrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJdbcRepository jdbcRepository;

    @Autowired
    private JwtService jwtService;

    // Local instance rather than Spring's autoconfigured bean: only used to build request
    // bodies for this test, doesn't need to match whatever Jackson major version the app's
    // production HttpMessageConverter uses internally (see ProductPersistenceAdapter's
    // identical rationale for not relying on the ambient Spring Jackson bean).
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        jdbcRepository.deleteAll();
        adminToken = jwtService.generateToken("test-admin", java.util.Set.of("ADMIN"));
        userToken = jwtService.generateToken("test-user", java.util.Set.of("USER"));
    }

    // ── POST /api/v1/products ───────────────────────────────────────────────

    @Test
    void createProduct_shouldReturn201_withGeneratedId() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO("SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.sku", is("SKU-001")))
                .andExpect(jsonPath("$.name", is("Wireless Mouse")))
                .andExpect(jsonPath("$.price", is(29.99)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void createProduct_shouldReturn409_whenSkuAlreadyExists() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));
        ProductRequestDTO request = new ProductRequestDTO("SKU-001", "Another Mouse", "desc", "Electronics", 19.99, 10, true);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void createProduct_shouldReturn400_whenRequestIsInvalid() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO("", null, "desc", "cat", -1.0, -1, null);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void createProduct_shouldReturn401_whenNoToken() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO("SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_shouldReturn403_whenUserRole() throws Exception {
        ProductRequestDTO request = new ProductRequestDTO("SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/products/active ─────────────────────────────────────────

    @Test
    void listActiveProducts_shouldReturn200_withOnlyActiveProducts() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Mouse", "desc", "Electronics", 29.99, 150, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 0, false));
        jdbcRepository.save(new ProductEntity(null, "SKU-003", "Hub", "desc", "Accessories", 24.50, 80, true));

        mockMvc.perform(get("/api/v1/products/active")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].active", is(true)))
                .andExpect(jsonPath("$.content[1].active", is(true)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void listActiveProducts_shouldReturn200_withEmptyArray_whenNoActiveProducts() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 0, false));

        mockMvc.perform(get("/api/v1/products/active")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void listActiveProducts_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/products/active"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/products/inactive ───────────────────────────────────────

    @Test
    void listInactiveProducts_shouldReturn200_withOnlyInactiveProducts() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Mouse", "desc", "Electronics", 29.99, 150, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "Keyboard", "desc", "Electronics", 79.99, 0, false));
        jdbcRepository.save(new ProductEntity(null, "SKU-003", "Hub", "desc", "Accessories", 24.50, 80, false));

        mockMvc.perform(get("/api/v1/products/inactive")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].active", is(false)))
                .andExpect(jsonPath("$.content[1].active", is(false)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void listInactiveProducts_shouldReturn200_withEmptyArray_whenNoInactiveProducts() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Mouse", "desc", "Electronics", 29.99, 150, true));

        mockMvc.perform(get("/api/v1/products/inactive")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void listInactiveProducts_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/products/inactive"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/products/search ──────────────────────────────────────────

    @Test
    void searchByNamePrefix_shouldReturn200_withMatchingProducts() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-002", "wireless Keyboard", "desc", "Electronics", 79.99, 10, true));
        jdbcRepository.save(new ProductEntity(null, "SKU-003", "USB Hub", "desc", "Accessories", 24.50, 80, true));

        mockMvc.perform(get("/api/v1/products/search").param("prefix", "wireless")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    // ── GET /api/v1/products/sku/{sku} ───────────────────────────────────────

    @Test
    void findBySku_shouldReturn200_whenProductExists() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        mockMvc.perform(get("/api/v1/products/sku/{sku}", "SKU-001")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("SKU-001")));
    }

    @Test
    void findBySku_shouldReturn404_whenProductNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/sku/{sku}", "SKU-NOPE")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/products/{id} ───────────────────────────────────────────

    @Test
    void findById_shouldReturn200_whenProductExists() throws Exception {
        ProductEntity saved = jdbcRepository
                .save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        mockMvc.perform(get("/api/v1/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId())))
                .andExpect(jsonPath("$.name", is("Wireless Mouse")));
    }

    @Test
    void findById_shouldReturn404_whenProductNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 9999)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void findById_shouldReturn400_whenIdIsNegative() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", -1)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_shouldReturn400_whenIdIsZero() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 0)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/v1/products/{id} ───────────────────────────────────────────

    @Test
    void updateProduct_shouldReturn200_withUpdatedData_whenExists() throws Exception {
        ProductEntity saved = jdbcRepository
                .save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        ProductUpdateRequestDTO request =
                new ProductUpdateRequestDTO("SKU-001", "Wireless Mouse Pro", "desc2", "Electronics", 34.99, 120, false);

        mockMvc.perform(put("/api/v1/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId())))
                .andExpect(jsonPath("$.name", is("Wireless Mouse Pro")))
                .andExpect(jsonPath("$.price", is(34.99)))
                .andExpect(jsonPath("$.active", is(false)));
    }

    @Test
    void updateProduct_shouldReturn404_whenNotFound() throws Exception {
        ProductUpdateRequestDTO request =
                new ProductUpdateRequestDTO("SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true);

        mockMvc.perform(put("/api/v1/products/{id}", 9999)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void updateProduct_shouldReturn400_whenRequestIsInvalid() throws Exception {
        ProductUpdateRequestDTO request =
                new ProductUpdateRequestDTO("", null, "desc", "cat", -1.0, -1, null);

        mockMvc.perform(put("/api/v1/products/{id}", 1)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errores").isArray());
    }

    // ── DELETE /api/v1/products/{id} ────────────────────────────────────────

    @Test
    void deleteProduct_shouldReturn204_whenExists() throws Exception {
        ProductEntity saved = jdbcRepository
                .save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        mockMvc.perform(delete("/api/v1/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", 9999)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ── POST /api/v1/products/import ── moved to ProductImportControllerIntegrationTest
    // (now an async Spring Batch job — see infrastructure.batch / .web.ProductImportController)

    // ── GET /api/v1/products/report/pdf ─────────────────────────────────────

    @Test
    void downloadPdfReport_shouldReturnPdf_containingProductData() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        byte[] pdfBytes = mockMvc.perform(get("/api/v1/products/report/pdf")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("products-report.pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("SKU-001").contains("Wireless Mouse").contains("Reporte de Productos");
        }
    }

    @Test
    void downloadPdfReport_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/products/report/pdf"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/v1/products/report/excel ───────────────────────────────────

    @Test
    void downloadExcelReport_shouldReturnXlsx_containingProductData() throws Exception {
        jdbcRepository.save(new ProductEntity(null, "SKU-001", "Wireless Mouse", "desc", "Electronics", 29.99, 150, true));

        byte[] xlsxBytes = mockMvc.perform(get("/api/v1/products/report/excel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("products-report.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("SKU");

            Row dataRow = sheet.getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("SKU-001");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("Wireless Mouse");
            assertThat(dataRow.getCell(3).getNumericCellValue()).isEqualTo(29.99);
            assertThat(dataRow.getCell(4).getNumericCellValue()).isEqualTo(150);
        }
    }

    @Test
    void downloadExcelReport_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/products/report/excel"))
                .andExpect(status().isUnauthorized());
    }
}
