package com.druvu.web.api.auth;

/**
 * How people sign in, chosen once per application. Machines sign in differently, see {@link BearerAuthentication}.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public sealed interface Authentication permits BasicAuthentication, OpenIdAuthentication {}
