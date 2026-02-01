package com.chibashr.allthewebhooks.webhook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link JsonEscaper}.
 */
class JsonEscaperTest {

    @Test
    void escape_null_returnsEmpty() {
        assertEquals("", JsonEscaper.escape(null));
    }

    @Test
    void escape_empty_returnsEmpty() {
        assertEquals("", JsonEscaper.escape(""));
    }

    @Test
    void escape_plainText_unchanged() {
        assertEquals("hello world", JsonEscaper.escape("hello world"));
    }

    @Test
    void escape_backslash_escaped() {
        assertEquals("\\\\", JsonEscaper.escape("\\"));
    }

    @Test
    void escape_doubleQuote_escaped() {
        assertEquals("\\\"", JsonEscaper.escape("\""));
    }

    @Test
    void escape_newline_escaped() {
        assertEquals("\\n", JsonEscaper.escape("\n"));
    }

    @Test
    void escape_carriageReturn_escaped() {
        assertEquals("\\r", JsonEscaper.escape("\r"));
    }

    @Test
    void escape_tab_escaped() {
        assertEquals("\\t", JsonEscaper.escape("\t"));
    }

    @Test
    void escape_multipleSpecialChars_allEscaped() {
        assertEquals("line1\\nline2\\t\\\"quoted\\\"", JsonEscaper.escape("line1\nline2\t\"quoted\""));
    }
}
