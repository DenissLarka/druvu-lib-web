package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;

/** Answers {@code /broken}; its template does not parse, which is the point. */
public final class BrokenHandler implements HttpHandler {}
