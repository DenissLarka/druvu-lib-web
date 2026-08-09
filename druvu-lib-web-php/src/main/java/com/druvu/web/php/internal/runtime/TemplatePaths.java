package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.PhpProcessingException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Where an include path points.
 *
 * <p>A relative path resolves against the file doing the including, which is what lets a partial sit next to the page
 * that uses it. The walk up is done here rather than by {@code java.nio.file.Path} so that one rule can be stated and
 * enforced plainly: a path that climbs above the template root is refused rather than resolved. Templates are content,
 * and content must not be able to name {@code ../../etc/passwd}.
 *
 * @author Deniss Larka
 */
public final class TemplatePaths {

    private TemplatePaths() {}

    /**
     * @param requested the path as the template wrote it
     * @param currentTemplate the path of the template doing the including
     * @throws PhpProcessingException if the path climbs out of the template root
     */
    public static String resolve(String requested, String currentTemplate) {
        String combined = requested.startsWith("/") ? requested : directoryOf(currentTemplate) + requested;

        Deque<String> segments = new ArrayDeque<>();
        for (String segment : combined.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (segments.isEmpty()) {
                    throw new PhpProcessingException("Include path climbs above the template root: " + requested);
                }
                segments.removeLast();
            } else {
                segments.addLast(segment);
            }
        }
        return "/" + String.join("/", segments);
    }

    private static String directoryOf(String path) {
        int lastSlash = path == null ? -1 : path.lastIndexOf('/');
        return lastSlash < 0 ? "/" : path.substring(0, lastSlash + 1);
    }
}
