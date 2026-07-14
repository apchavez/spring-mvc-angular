package com.apchavez.products.infrastructure.web;

import com.apchavez.products.application.ProductApplicationService;
import com.apchavez.products.domain.model.Product;
import com.apchavez.products.infrastructure.mapper.ProductMapper;
import com.apchavez.products.infrastructure.web.dto.PageResponse;
import com.apchavez.products.infrastructure.web.dto.ProductRequestDTO;
import com.apchavez.products.infrastructure.web.dto.ProductResponseDTO;
import com.apchavez.products.infrastructure.web.dto.ProductUpdateRequestDTO;
import com.apchavez.products.infrastructure.web.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Operaciones de gestión de productos")
public class ProductController {

    private final ProductApplicationService applicationService;
    private final ProductMapper mapper;

    public ProductController(ProductApplicationService applicationService, ProductMapper mapper) {
        this.applicationService = applicationService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Crear producto", description = "Crea un nuevo producto con ID generado automáticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos (Bean Validation)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un producto con ese SKU",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Violación de regla de dominio",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        Product saved = applicationService.createProduct(mapper.toDomain(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponseDTO(saved));
    }

    @GetMapping("/active")
    @Operation(summary = "Listar productos activos", description = "Retorna productos activos. Paginado con page (0-based) y size (max 100).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de productos activos")
    })
    public PageResponse<ProductResponseDTO> listActiveProducts(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
        long total = applicationService.countActiveProducts();
        List<ProductResponseDTO> content = applicationService.listActiveProducts(page, size)
                .stream().map(mapper::toResponseDTO).toList();
        return PageResponse.of(content, page, size, total);
    }

    @GetMapping("/inactive")
    @Operation(summary = "Listar productos inactivos", description = "Retorna productos inactivos (desactivados). Paginado con page (0-based) y size (max 100).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de productos inactivos")
    })
    public PageResponse<ProductResponseDTO> listInactiveProducts(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
        long total = applicationService.countInactiveProducts();
        List<ProductResponseDTO> content = applicationService.listInactiveProducts(page, size)
                .stream().map(mapper::toResponseDTO).toList();
        return PageResponse.of(content, page, size, total);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar productos por prefijo de nombre", description = "Búsqueda insensible a mayúsculas por prefijo de nombre. Paginado con page (0-based) y size (max 100).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de productos encontrados")
    })
    public PageResponse<ProductResponseDTO> searchByNamePrefix(
            @RequestParam @NotBlank String prefix,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
        long total = applicationService.countByNamePrefix(prefix);
        List<ProductResponseDTO> content = applicationService.searchByNamePrefix(prefix, page, size)
                .stream().map(mapper::toResponseDTO).toList();
        return PageResponse.of(content, page, size, total);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Buscar producto por SKU", description = "Retorna el producto con el SKU indicado o 404 si no existe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDTO> findBySku(@PathVariable @NotBlank String sku) {
        return applicationService.findBySku(sku)
                .map(product -> ResponseEntity.ok(mapper.toResponseDTO(product)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar producto por ID", description = "Retorna el producto con el ID indicado o 404 si no existe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido (debe ser mayor que cero)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDTO> findById(
            @PathVariable @Positive(message = "El ID debe ser mayor que cero") Integer id) {
        Product product = applicationService.findById(id);
        return ResponseEntity.ok(mapper.toResponseDTO(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", description = "Reemplaza todos los datos del producto con el ID indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado",
                    content = @Content(schema = @Schema(implementation = ProductResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID o campos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Violación de regla de dominio",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable @Positive(message = "El ID debe ser mayor que cero") Integer id,
            @Valid @RequestBody ProductUpdateRequestDTO dto) {
        Product updated = applicationService.updateProduct(id, mapper.toDomain(dto));
        return ResponseEntity.ok(mapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto", description = "Elimina el producto con el ID indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "400", description = "ID inválido (debe ser mayor que cero)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteProduct(
            @PathVariable @Positive(message = "El ID debe ser mayor que cero") Integer id) {
        applicationService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
