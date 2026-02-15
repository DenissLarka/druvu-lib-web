package com.druvu.web.example;

import com.druvu.web.example.handlers.*;
import org.slf4j.simple.SimpleLogger;

import com.druvu.web.api.auth.AuthConfig;
import com.druvu.web.core.WebBoot;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.api.config.WebConfig;

/**
 * @author : Deniss Larka
 * <br/>on 21 Apr 2024
 **/
public final class WebBootExample {

	public static final int PORT = 8081;

	private WebBootExample() {
	}

	static void main(String[] args) {
		logConfig();

		final WebConfig webConfig = WebConfig.builder()
				.port(PORT)
				.urlConfig(UrlConfig.from(Example.class))
				.urlConfig(UrlConfig.from(ExampleTable.class))
				.urlConfig(UrlConfig.from(ExampleSocketHandler.class))
				.urlConfig(UrlConfig.from(ExampleJsonHandler.class))
				.urlConfig(UrlConfig.from(WsHandler.class))
				.authConfig(AuthConfig.builder()
						.basicAuth()
						.user("user", "pass", "generic:permission")
						.user("admin", "admin", "generic:permission", "admin:permission")
						.build())
				.build();

		WebBoot boot = new WebBoot(webConfig);
		boot.start("/web-test");
	}

	private static void logConfig() {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
	}
}
