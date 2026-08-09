package com.druvu.web.php.internal;

/**
 * A template could not be read: an unterminated string, an unknown character, a construct the dialect does not have.
 *
 * <p>Always carries the place it happened, because the one thing that makes a template error usable is the file and
 * line — not an offset into the fragment the old engine used to report.
 *
 * @author Deniss Larka
 */
public final class PhpSyntaxException extends PhpProcessingException {

    private static final long serialVersionUID = 1L;

    private final transient Location location;

    public PhpSyntaxException(Location location, String message) {
        super(location + ": " + message);
        this.location = location;
    }

    /** Where the template went wrong. */
    public Location location() {
        return location;
    }
}
