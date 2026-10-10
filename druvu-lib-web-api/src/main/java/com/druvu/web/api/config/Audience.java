package com.druvu.web.api.config;

/**
 * Who a route is for, which decides how a caller signs in: people arrive through a browser and the application's people
 * authentication; machines present a bearer token.
 *
 * @author Deniss Larka <br>
 *     on 10 Oct 2026
 */
public enum Audience {
    PEOPLE,
    MACHINES
}
