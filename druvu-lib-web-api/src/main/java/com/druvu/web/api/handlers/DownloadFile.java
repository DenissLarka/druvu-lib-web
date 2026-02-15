package com.druvu.web.api.handlers;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * @author Deniss Larka
 * on 14 July 2022
 */
@Builder
@Getter
public final class DownloadFile {
	@NonNull
	private final String filename;
	@NonNull
	private final String contentType;
	@NonNull
	private final byte[] content;

	public void send(HttpServletResponse res) {
		send(res, this);
	}

	@SneakyThrows
	public static void send(HttpServletResponse res, DownloadFile file) {
		res.setHeader("Content-Disposition", "attachment; filename=" + file.filename());
		res.setContentType(file.contentType());
		res.setContentLengthLong(file.content().length);
		res.getOutputStream().write(file.content());
	}

}
