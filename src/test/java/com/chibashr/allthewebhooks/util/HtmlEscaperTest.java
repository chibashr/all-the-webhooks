package com.chibashr.allthewebhooks.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link HtmlEscaper}.
 */
class HtmlEscaperTest {

    @Test
    void escape_null_returnsEmpty() {
        assertEquals("", HtmlEscaper.escape(null));
    }

    @Test
    void escape_empty_returnsEmpty() {
        assertEquals("", HtmlEscaper.escape(""));
    }

    @Test
    void escape_plainText_unchanged() {
        assertEquals("hello world", HtmlEscaper.escape("hello world"));
    }

    @Test
    void escape_ampersand_escaped() {
        assertEquals("&amp;", HtmlEscaper.escape("&"));
        assertEquals("a&amp;b", HtmlEscaper.escape("a&b"));
    }

    @Test
    void escape_lessThan_escaped() {
        assertEquals("&lt;", HtmlEscaper.escape("<"));
        assertEquals("&lt;script&gt;", HtmlEscaper.escape("<script>"));
    }

    @Test
    void escape_greaterThan_escaped() {
        assertEquals("&gt;", HtmlEscaper.escape(">"));
    }

    @Test
    void escape_doubleQuote_escaped() {
        assertEquals("&quot;", HtmlEscaper.escape("\""));
    }

    @Test
    void escape_singleQuote_escaped() {
        assertEquals("&#39;", HtmlEscaper.escape("'"));
    }

    @Test
    void escape_multipleSpecialChars_allEscaped() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;&amp;&lt;/a&gt;",
                HtmlEscaper.escape("<a href=\"x\">&</a>"));
    }
}
