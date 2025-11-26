package com.kitchensink.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CursorPageResponse DTO Tests")
class CursorPageResponseTest {

    private CursorPageResponse<String> cursorPage;

    @BeforeEach
    void setUp() {
        cursorPage = new CursorPageResponse<>();
    }

    @Test
    @DisplayName("Should set and get all fields")
    void testGettersAndSetters() {
        List<String> content = List.of("item1", "item2");
        cursorPage.setContent(content);
        cursorPage.setNextCursor("next-cursor");
        cursorPage.setPreviousCursor("prev-cursor");
        cursorPage.setHasNext(true);
        cursorPage.setHasPrevious(false);
        cursorPage.setSize(2);

        assertThat(cursorPage.getContent()).isEqualTo(content);
        assertThat(cursorPage.getNextCursor()).isEqualTo("next-cursor");
        assertThat(cursorPage.getPreviousCursor()).isEqualTo("prev-cursor");
        assertThat(cursorPage.isHasNext()).isTrue();
        assertThat(cursorPage.isHasPrevious()).isFalse();
        assertThat(cursorPage.getSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should create with constructor")
    void testConstructor() {
        List<String> content = List.of("item1");
        CursorPageResponse<String> newPage = new CursorPageResponse<>(
                content, "next", "prev", true, false, 1);

        assertThat(newPage.getContent()).isEqualTo(content);
        assertThat(newPage.getNextCursor()).isEqualTo("next");
        assertThat(newPage.getPreviousCursor()).isEqualTo("prev");
        assertThat(newPage.isHasNext()).isTrue();
        assertThat(newPage.isHasPrevious()).isFalse();
        assertThat(newPage.getSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle empty content")
    void testEmptyContent() {
        cursorPage.setContent(Collections.emptyList());
        cursorPage.setSize(0);
        cursorPage.setHasNext(false);
        cursorPage.setHasPrevious(false);

        assertThat(cursorPage.getContent()).isEmpty();
        assertThat(cursorPage.getSize()).isEqualTo(0);
        assertThat(cursorPage.isHasNext()).isFalse();
        assertThat(cursorPage.isHasPrevious()).isFalse();
    }
}

