package com.druvu.web.example;

import com.druvu.web.api.auth.ApiTokens;
import com.druvu.web.api.auth.TokenStore;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The demo's token store: in memory, hashes only, exactly what a real one would keep in a database.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public final class ExampleTokens implements TokenStore {

    public static final ExampleTokens STORE = new ExampleTokens();

    private final Map<String, String> subjectsByHash = new ConcurrentHashMap<>();

    private ExampleTokens() {}

    /** Hands out a fresh token for the subject and remembers only its hash; the caller shows the token once. */
    public String issue(String subject) {
        String token = ApiTokens.generate();
        subjectsByHash.put(ApiTokens.hash(token), subject);
        return token;
    }

    public long issuedFor(String subject) {
        return subjectsByHash.values().stream().filter(subject::equals).count();
    }

    @Override
    public Optional<String> subjectOf(String presented) {
        return Optional.ofNullable(subjectsByHash.get(ApiTokens.hash(presented)));
    }
}
