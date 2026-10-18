package com.druvu.web.core.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** The download header: a plain name in quotes alone, any other name also in its UTF-8 form. */
public class TestContentDisposition {

    @Test
    public void aPlainNameIsQuotedAndThatIsAll() {
        assertThat(ContentDisposition.attachment("report.csv")).isEqualTo("attachment; filename=\"report.csv\"");
        assertThat(ContentDisposition.attachment("my report.csv")).isEqualTo("attachment; filename=\"my report.csv\"");
    }

    @Test
    public void aNameBeyondAsciiAlsoTravelsInUtf8() {
        assertThat(ContentDisposition.attachment("Rechnung März.pdf"))
                .isEqualTo("attachment; filename=\"Rechnung M_rz.pdf\"; filename*=UTF-8''Rechnung%20M%C3%A4rz.pdf");
    }

    @Test
    public void aQuoteOrABackslashCannotBreakTheQuotedName() {
        assertThat(ContentDisposition.attachment("a\"b\\c.txt"))
                .isEqualTo("attachment; filename=\"a_b_c.txt\"; filename*=UTF-8''a%22b%5Cc.txt");
    }
}
