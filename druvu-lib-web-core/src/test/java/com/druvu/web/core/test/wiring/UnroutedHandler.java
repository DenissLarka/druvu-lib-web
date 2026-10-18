package com.druvu.web.core.test.wiring;

import com.druvu.web.api.handlers.HttpHandler;

/** A handler no configuration registers, so a redirect to it has nowhere to go. */
public final class UnroutedHandler implements HttpHandler {}
