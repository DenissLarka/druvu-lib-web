package com.druvu.web.php.internal.runtime;

import com.druvu.web.php.internal.Location;

/**
 * Something the template did that PHP would have mentioned but not stopped for: an undefined variable, a missing array
 * key, a string offset past the end.
 *
 * <p>Collected rather than printed. Where they surface — a log, a debug panel, nowhere — is the host's business, and
 * writing them into the page is how PHP produces those broken layouts with a warning wedged into the markup.
 *
 * @author Deniss Larka
 */
public record Diagnostic(Location location, String message) {

    @Override
    public String toString() {
        return location + ": " + message;
    }
}
