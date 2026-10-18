package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.DownloadFile;
import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import java.nio.file.Path;

/** Answers {@code /download-disk} with the file in the global object {@code downloadable}, streamed from disk. */
public final class DownloadDiskHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        Path file = request.globalAttributes().get("downloadable");
        response.commit(DownloadFile.of(file));
    }
}
