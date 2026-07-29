package com.apchavez.products.infrastructure.web;

import com.apchavez.products.infrastructure.auth.DemoUserStore;
import com.apchavez.products.infrastructure.auth.InvalidCredentialsException;
import com.apchavez.products.infrastructure.config.JwtService;
import com.apchavez.products.infrastructure.web.dto.LoginRequestDTO;
import com.apchavez.products.infrastructure.web.dto.LoginResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticación — emite JWTs para los usuarios demo")
public class AuthController {

    private final DemoUserStore userStore;
    private final JwtService jwtService;
    private final long expirationMs;

    public AuthController(
            DemoUserStore userStore,
            JwtService jwtService,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        this.userStore = userStore;
        this.jwtService = jwtService;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica a un usuario demo y retorna un JWT firmado. "
            + "Credenciales demo: admin/admin123 (rol ADMIN), user/user123 (rol USER).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Error de validación en los campos"),
            @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos")
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        Set<String> roles = userStore.authenticate(request.username(), request.password())
                .orElseThrow(InvalidCredentialsException::new);
        String token = jwtService.generateToken(request.username(), roles);
        return ResponseEntity.ok(new LoginResponseDTO(token, "Bearer", expirationMs / 1000, request.username(), roles));
    }
}
