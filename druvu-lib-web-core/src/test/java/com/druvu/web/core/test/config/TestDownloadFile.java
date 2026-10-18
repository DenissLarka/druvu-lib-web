package com.druvu.web.core.test.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.web.api.handlers.DownloadFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.Test;

/** A download made from a file on disk or from bytes: its name, type and content, and what it refuses. */
public class TestDownloadFile {

    @Test
    public void aFileOnDiskKeepsItsNameAndIsReadWhenWritten() throws IOException {
        Path file = Files.createTempFile("druvu-download", ".bin");
        try {
            Files.write(file, new byte[] {1, 2, 3});
            DownloadFile download = DownloadFile.of(file);
            assertThat(download.filename()).isEqualTo(String.valueOf(file.getFileName()));
            assertThat(download.contentType()).isEqualTo(DownloadFile.BINARY);
            assertThat(download.length()).isEqualTo(3);
            assertThat(written(download)).containsExactly(1, 2, 3);
        } finally {
            Files.delete(file);
        }
    }

    @Test
    public void bytesInMemoryAreCopiedSoTheCallerMayReuseTheArray() throws IOException {
        byte[] bytes = "a;b".getBytes(StandardCharsets.UTF_8);
        DownloadFile download = DownloadFile.of("orders.csv", "text/csv", bytes);
        bytes[0] = 'x';
        assertThat(download.contentType()).isEqualTo("text/csv");
        assertThat(new String(written(download), StandardCharsets.UTF_8)).isEqualTo("a;b");
    }

    @Test
    public void aDirectoryAndANameWithALineBreakAreRefused() throws IOException {
        Path directory = Files.createTempDirectory("druvu-download");
        try {
            assertThatThrownBy(() -> DownloadFile.of(directory)).isInstanceOf(IllegalArgumentException.class);
        } finally {
            Files.delete(directory);
        }
        assertThatThrownBy(() -> DownloadFile.of("a\r\nb.txt", "text/plain", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] written(DownloadFile download) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        download.writeTo(out);
        return out.toByteArray();
    }
}
