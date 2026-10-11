package com.druvu.web.example.mcp;

import com.druvu.web.api.auth.ApiTokens;
import com.druvu.web.api.auth.TokenStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * The one token this server accepts: generated at boot, shown once, kept only as its hash, which is the shape
 * {@link ApiTokens} is for. Whoever presents it is the subject {@code mcp-client}.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public final class BootToken implements TokenStore {

    static final String SUBJECT = "mcp-client";

    private final String token;
    private final byte[] hash;

    private BootToken(String token) {
        this.token = token;
        this.hash = ApiTokens.hash(token).getBytes(StandardCharsets.UTF_8);
    }

    /** A fresh token: 256 random bits. */
    public static BootToken generate() {
        return new BootToken(ApiTokens.generate());
    }

    /** The store for a token already known, which is what a test wants. */
    public static BootToken of(String token) {
        return new BootToken(token);
    }

    /** The token in clear, to show once at boot. */
    public String token() {
        return token;
    }

    @Override
    public Optional<String> subjectOf(String presented) {
        byte[] presentedHash = ApiTokens.hash(presented).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(presentedHash, hash) ? Optional.of(SUBJECT) : Optional.empty();
    }
}
