package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.ast.PhpTemplate;

/**
 * Where an {@code include} finds the template it names.
 *
 * <p>Deliberately parsed templates rather than text: it keeps the tree free of any knowledge of parsing, and it is the
 * seam a cache slots into later without a single include node changing.
 *
 * @author Deniss Larka
 */
@FunctionalInterface
public interface TemplateSource {

    /** Nothing can be included. What a render outside a container gets. */
    TemplateSource NONE = path -> null;

    /** The template at this path, or null when there is none. */
    PhpTemplate find(String path);
}
