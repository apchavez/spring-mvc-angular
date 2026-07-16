package com.apchavez.products.infrastructure.web.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    // ── size == 0 branch: guards against division by zero ──────────────────

    @Test
    void of_returns_single_total_page_when_size_is_zero() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 0, 42);

        assertThat(response.totalPages()).isEqualTo(1);
        // page (0) >= totalPages - 1 (0) -> last page
        assertThat(response.last()).isTrue();
    }

    // ── size != 0 branch: normal ceil-based page count ──────────────────────

    @Test
    void of_computes_total_pages_via_ceiling_division_when_size_is_positive() {
        // 45 elements / size 20 -> 3 pages (ceil(2.25) = 3), not an exact multiple
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 0, 20, 45);

        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void of_computes_total_pages_exactly_when_total_elements_is_a_multiple_of_size() {
        // 40 elements / size 20 -> exactly 2 pages, no remainder
        PageResponse<String> response = PageResponse.of(List.of(), 0, 20, 40);

        assertThat(response.totalPages()).isEqualTo(2);
    }

    // ── last == true branch: current page is the final page ─────────────────

    @Test
    void of_marks_last_page_as_last_when_page_equals_final_index() {
        // 45 elements / size 20 -> 3 pages (indices 0,1,2); page 2 is last
        PageResponse<String> response = PageResponse.of(List.of("x"), 2, 20, 45);

        assertThat(response.last()).isTrue();
    }

    // ── last == false branch: current page precedes the final page ──────────

    @Test
    void of_marks_first_page_as_not_last_when_more_pages_remain() {
        // 45 elements / size 20 -> 3 pages; page 0 is not last
        PageResponse<String> response = PageResponse.of(List.of("a", "b"), 0, 20, 45);

        assertThat(response.last()).isFalse();
    }

    // ── Empty result set: no elements at all ─────────────────────────────────

    @Test
    void of_treats_empty_result_set_as_its_own_last_page() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 20, 0);

        assertThat(response.content()).isEmpty();
        assertThat(response.last()).isTrue();
    }

    // ── Fields are carried through unchanged ─────────────────────────────────

    @Test
    void of_preserves_content_page_size_and_total_elements() {
        List<String> content = List.of("a", "b", "c");

        PageResponse<String> response = PageResponse.of(content, 1, 3, 10);

        assertThat(response.content()).containsExactly("a", "b", "c");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(10);
    }
}
