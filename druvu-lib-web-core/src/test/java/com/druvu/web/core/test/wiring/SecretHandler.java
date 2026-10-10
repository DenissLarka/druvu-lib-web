package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;

/** Answers {@code /secret}; registered with a required permission, so the dispatcher must authenticate first. */
public final class SecretHandler implements HttpHandler {}
