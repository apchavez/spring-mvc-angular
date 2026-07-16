package com.apchavez.products.infrastructure.web;

import com.apchavez.products.AbstractIntegrationTest;
import com.apchavez.products.infrastructure.config.JwtService;
import com.apchavez.products.infrastructure.persistence.ProductJdbcRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real, end-to-end async flow: POST returns 202 immediately, the Job runs on a
 * background thread, and the test polls GET until Spring Batch reports COMPLETED — same
 * contract as spring-webflux-angular's Postman collection ("Importar productos" then "Estado
 * de importación").
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ProductImportControllerIntegrationTest extends AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJdbcRepository jdbcRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        jdbcRepository.deleteAll();
        adminToken = jwtService.generateToken("test-admin", java.util.Set.of("ADMIN"));
        userToken = jwtService.generateToken("test-user", java.util.Set.of("USER"));
    }

    @Test
    void importProducts_shouldCreateAllRows_whenCsvIsValid() throws Exception {
        String csv = "sku,name,description,category,price,stock,active\n"
                + "SKU-100,Mouse,desc,Electronics,29.99,150,true\n"
                + "SKU-101,Keyboard,desc,Electronics,79.99,10,true\n";

        long jobExecutionId = launchImport(csv);

        JsonNode status = awaitCompletion(jobExecutionId);

        assertThat(status.get("imported").asInt()).isEqualTo(2);
        assertThat(status.get("failed").asInt()).isZero();
        assertThat(jdbcRepository.count()).isEqualTo(2);
    }

    @Test
    void importProducts_shouldReportInvalidRows_withoutAbortingTheRest() throws Exception {
        String csv = "sku,name,description,category,price,stock,active\n"
                + "SKU-100,Mouse,desc,Electronics,29.99,150,true\n"
                + ",NoSku,desc,Electronics,10.0,5,true\n"
                + "SKU-101,Keyboard,desc,Electronics,79.99,10,true\n";

        long jobExecutionId = launchImport(csv);

        JsonNode status = awaitCompletion(jobExecutionId);

        assertThat(status.get("imported").asInt()).isEqualTo(2);
        assertThat(status.get("failed").asInt()).isEqualTo(1);
        assertThat(status.get("errors").get(0).get("row").asInt()).isEqualTo(3);
        assertThat(jdbcRepository.count()).isEqualTo(2);
    }

    @Test
    void importProducts_shouldReturn400_whenFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/v1/products/import").file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importProducts_shouldReturn401_whenNoToken() throws Exception {
        MockMultipartFile file = csvFile("sku,name,description,category,price,stock,active\n");

        mockMvc.perform(multipart("/api/v1/products/import").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importProducts_shouldReturn403_whenUserRole() throws Exception {
        MockMultipartFile file = csvFile("sku,name,description,category,price,stock,active\n");

        mockMvc.perform(multipart("/api/v1/products/import").file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getImportStatus_shouldReturn404_whenJobExecutionDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/products/import/{id}", 999999)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    private MockMultipartFile csvFile(String csv) {
        return new MockMultipartFile("file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }

    private long launchImport(String csv) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/products/import").file(csvFile(csv))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobExecutionId").exists())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("jobExecutionId").asLong();
    }

    private JsonNode awaitCompletion(long jobExecutionId) {
        return await().atMost(java.time.Duration.ofSeconds(10)).until(() -> {
            MvcResult result = mockMvc.perform(get("/api/v1/products/import/{id}", jobExecutionId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            return "COMPLETED".equals(body.get("status").asText()) ? body : null;
        }, org.hamcrest.Matchers.notNullValue());
    }
}
