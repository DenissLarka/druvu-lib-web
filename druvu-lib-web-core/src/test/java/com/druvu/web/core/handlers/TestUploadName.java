package com.druvu.web.core.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** The name of an uploaded file: no directory part and no control characters, whatever the browser sent. */
public class TestUploadName {

    @Test
    public void aPlainNameIsKept() {
        assertThat(PartUpload.usableName("report.pdf")).isEqualTo("report.pdf");
        assertThat(PartUpload.usableName("Rechnung März.pdf")).isEqualTo("Rechnung März.pdf");
    }

    @Test
    public void aDirectoryPartIsDroppedOnEitherSlash() {
        assertThat(PartUpload.usableName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(PartUpload.usableName("C:\\Users\\ada\\report.pdf")).isEqualTo("report.pdf");
    }

    @Test
    public void aNameWithNothingUsableLeftBecomesUpload() {
        assertThat(PartUpload.usableName("..")).isEqualTo("upload");
        assertThat(PartUpload.usableName("dir/")).isEqualTo("upload");
        assertThat(PartUpload.usableName("\r\n")).isEqualTo("upload");
        assertThat(PartUpload.usableName(null)).isEqualTo("upload");
    }

    @Test
    public void controlCharactersAreDropped() {
        assertThat(PartUpload.usableName("a\r\nb.txt")).isEqualTo("ab.txt");
    }
}
