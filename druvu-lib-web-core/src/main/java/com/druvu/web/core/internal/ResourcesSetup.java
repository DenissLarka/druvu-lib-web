package com.druvu.web.core.internal;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.SneakyThrows;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * The base resource static files are served from, laid out like the URL space: a directory on disk when given, the
 * classpath's {@code webapp}, and {@code META-INF/resources} of every jar, where WebJars keep their files.
 *
 * @author Deniss Larka on 11 February 2022
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
        return RcUtils.resources("META-INF/resources");
    }

    @SneakyThrows
    private Resource webappResource() {
        return RcUtils.resource("webapp");
    }
}
