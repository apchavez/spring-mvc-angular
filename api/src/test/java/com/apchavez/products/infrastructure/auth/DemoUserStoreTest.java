package com.apchavez.products.infrastructure.auth;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DemoUserStoreTest {

    private final DemoUserStore userStore = new DemoUserStore();

    @Test
    void authenticate_returnsAdminAndUserRoles_whenAdminCredentialsAreCorrect() {
        Optional<Set<String>> roles = userStore.authenticate("admin", "admin123");

        assertThat(roles).contains(Set.of("ADMIN", "USER"));
    }

    @Test
    void authenticate_returnsUserRole_whenUserCredentialsAreCorrect() {
        Optional<Set<String>> roles = userStore.authenticate("user", "user123");

        assertThat(roles).contains(Set.of("USER"));
    }

    @Test
    void authenticate_returnsEmpty_whenPasswordIsWrong() {
        Optional<Set<String>> roles = userStore.authenticate("admin", "wrong-password");

        assertThat(roles).isEmpty();
    }

    @Test
    void authenticate_returnsEmpty_whenUsernameDoesNotExist() {
        Optional<Set<String>> roles = userStore.authenticate("nobody", "admin123");

        assertThat(roles).isEmpty();
    }
}
