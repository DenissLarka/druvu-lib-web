package com.druvu.web.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

import lombok.SneakyThrows;

/**
 * @author Deniss Larka
 */
public final class RcUtils {

	private RcUtils() {
	}

	@SneakyThrows
	public static Resource resources(String resource) {
		Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(resource);
		List<Resource> result = new ArrayList<>();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			String baseUrlStr = url.toExternalForm();
			result.add(ResourceFactory.lifecycle().newResource(baseUrlStr));
		}
		return ResourceFactory.combine(result);
	}

	public static Resource resource(String resource) throws IOException {
		Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(resource);
		if (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			String baseUrlStr = url.toExternalForm();
			return ResourceFactory.lifecycle().newResource(baseUrlStr);
		}
		return null;
	}

	public static URL resourceUrl(String resource) {
		return Thread.currentThread().getContextClassLoader().getResource(resource);
	}

	public static InputStream resourceStream(String resource) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
	}
}
