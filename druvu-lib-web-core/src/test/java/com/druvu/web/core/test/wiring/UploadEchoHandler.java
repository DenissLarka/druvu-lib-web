package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.druvu.web.api.handlers.UploadedFile;

/**
 * Answers {@code /upload-echo} with what arrived: the text field {@code note}, what the parameters say of the file
 * field, the file found under {@code file}, and every upload with its size and SHA-256.
 */
public final class UploadEchoHandler implements HttpHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        StringBuilder echo = new StringBuilder();
        echo.append("note=").append(request.paramInfo().get("note")).append('\n');
        echo.append("file-param=").append(request.paramInfo().get("file")).append('\n');
        echo.append("upload(file)=")
                .append(request.upload("file").map(UploadedFile::filename).orElse("-"))
                .append('\n');
        for (UploadedFile file : request.uploads()) {
            echo.append(file.fieldName())
                    .append('|')
                    .append(file.filename())
                    .append('|')
                    .append(file.contentType())
                    .append('|')
                    .append(file.size())
                    .append('|')
                    .append(Sha256.hex(file.content()))
                    .append('\n');
        }
        response.commit("text/plain", echo.toString());
    }
}
