/**
 * Internal implementation for druvu-lib-web core: the dispatcher, the context and connector setup, the static files.
 *
 * <p>Jetty appears here only where it is the server: connectors, the servlet context and its resources, the error page,
 * the WebSocket container. Everything a handler touches speaks the servlet API.
 */
package com.druvu.web.core.internal;
