package com.druvu.web.core.internal;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * The base resource static files are served from, laid out like the URL space: a directory on disk when given, the
 * classpath's {@code webapp}, and {@code META-INF/resources} of every jar, where WebJars keep their files. An
 * application with none of the three, an API say, has no base resource and serves no static files.
 *
 * @author Deniss Larka on 11 February 2022
 */
public class ResourcesSetup {

    private final Path externalDirectory;

    public ResourcesSetup(Path externalDirectory) {
        this.externalDirectory = externalDirectory;
    }

    /** The merged base, or empty when there is nothing to serve. */
    @SneakyThrows
    public Optional<Resource> call() {
        List<Resource> parts = new ArrayList<>();
        if (externalDirectory != null) {
            parts.add(ResourceFactory.lifecycle().newResource(externalDirectory));
        }
        Resource webapp = RcUtils.resource("webapp");
        if (webapp != null) {
            parts.add(webapp);
        }
        parts.addAll(RcUtils.resourceList("META-INF/resources"));
        return parts.isEmpty() ? Optional.empty() : Optional.of(ResourceFactory.combine(parts));
    }
}
