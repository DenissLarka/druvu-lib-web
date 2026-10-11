package com.druvu.web.api.config;

/**
 * Who a route is for, which decides how a caller signs in.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public enum Audience {
    /** Anyone, signed in or not: nobody is asked to sign in. */
    EVERYONE,
    /** People, through a browser and the application's people authentication: Basic or OpenID Connect. */
    PEOPLE,
    /** Machines, presenting a bearer token. */
    MACHINES
}
