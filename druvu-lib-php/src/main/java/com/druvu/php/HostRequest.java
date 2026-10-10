package com.druvu.php;

import java.util.Map;

/**
 * What a template may know about the request it is rendered for.
 *
 * <p>This is the whole seam between the engine and whatever serves HTTP. The five reads become {@code $_GET},
 * {@code $_POST}, {@code $_COOKIE}, {@code $_SERVER} and the context path behind {@code context()}, {@code link()} and
 * {@code webjar()}; {@code $_REQUEST} is the query and the form together, the form winning. A render without a request
 * leaves the superglobals unset and makes the three path helpers fail by name.
 *
 * <p>Values are single: where a name arrives more than once, the last value is the one PHP would keep.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public interface HostRequest {

    /** The query string, decoded: {@code $_GET}. */
    Map<String, String> queryParameters();

    /** The form body, decoded: {@code $_POST}. */
    Map<String, String> formParameters();

    /** {@code $_COOKIE}. */
    Map<String, String> cookies();

    /** {@code $_SERVER}: {@code REQUEST_METHOD}, {@code REQUEST_URI} and the rest of what the host knows. */
    Map<String, String> serverVariables();

    /** The path the application is mounted at, empty at the root: what {@code context()} returns. */
    String contextPath();
}
