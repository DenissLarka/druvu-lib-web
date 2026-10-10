package com.druvu.web.api.auth;

import java.util.Objects;

/**
 * How machines sign in: a bearer token in the {@code Authorization} header, on the routes declared for machines.
 *
 * @param tokens who a token belongs to
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public record BearerAuthentication(TokenStore tokens) {

    public BearerAuthentication {
        Objects.requireNonNull(tokens, "tokens");
    }
}
