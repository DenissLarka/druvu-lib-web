package com.druvu.web.api.auth;

import java.util.Optional;

/**
 * Which subject a presented API token belongs to. The application owns the tokens: it hands them out, stores them
 * (hashed, see {@link ApiTokens}) and revokes them; this is only the lookup.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
@FunctionalInterface
public interface TokenStore {

    /** The subject this token belongs to, or empty when the token is unknown or revoked. */
    Optional<String> subjectOf(String token);
}
