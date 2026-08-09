package com.druvu.web.api.config;

import com.druvu.web.api.auth.AuthConfig;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

/**
 * @author : Deniss Larka <br>
 *     on 21 April 2024
 */
@Builder
@Getter
public class WebConfig {

    private final String host;
    private final int port;
    private final Path serveFromDirectory;

    @Singular
    private final Set<String> staticPaths = new HashSet<>(Set.of("/webjars/*", "/static/*"));

    @Singular
    private final Map<String, Object> globalObjects = new HashMap<>();

    @Singular
    private final List<UrlConfig> urlConfigs;

    private AuthConfig authConfig;
}
