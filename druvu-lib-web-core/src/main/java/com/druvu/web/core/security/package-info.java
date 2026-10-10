/**
 * What protects what. {@link com.druvu.web.core.security.SecuritySetup} is the one file to read: it turns an
 * {@code AuthConfig} and the routes into Jetty's session and security handlers, which run before any servlet, so the
 * dispatcher only ever reads an identity that Jetty has already established.
 */
package com.druvu.web.core.security;
