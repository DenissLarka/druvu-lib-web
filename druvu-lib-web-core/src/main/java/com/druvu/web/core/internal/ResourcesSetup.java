package com.druvu.web.core.internal;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

import lombok.SneakyThrows;

/**
 * @author Deniss Larka
 * on 11 February 2022
 */
public class ResourcesSetup implements Callable<Resource> {

	private final Path externalDirectory;

	public ResourcesSetup() {
		this(null);
	}

	public ResourcesSetup(Path externalDirectory) {
		this.externalDirectory = externalDirectory;
	}

	@Override
	public Resource call() {
		if (externalDirectory != null) {
			Resource externalResource = ResourceFactory.lifecycle().newResource(externalDirectory);
			return ResourceFactory.combine(externalResource, webappResource(), webjarResource());
		}
		return ResourceFactory.combine(webappResource(), webjarResource());
	}

	private Resource webjarResource() {
		return RcUtils.resources("META-INF/resources/webjars");
	}

	@SneakyThrows
	private Resource webappResource() {
		return ResourceFactory.combine(RcUtils.resource("webapp"), RcUtils.resource("webapp/static"));
	}
}
