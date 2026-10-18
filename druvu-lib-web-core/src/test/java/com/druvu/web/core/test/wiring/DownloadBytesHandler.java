package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.DownloadFile;
import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import java.nio.charset.StandardCharsets;

/** Answers {@code /download-bytes} with a small CSV made in memory, under a name beyond ASCII. */
public final class DownloadBytesHandler implements HttpHandler {

    static final String CONTENT = "a;b\n1;2\n";
    static final String FILE_NAME = "Rechnung März.csv";

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        response.commit(DownloadFile.of(FILE_NAME, "text/csv", CONTENT.getBytes(StandardCharsets.UTF_8)));
    }
}
