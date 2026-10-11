package com.druvu.web.example.mcp;

/**
 * What reading a request gives: a call for the server to answer, or the reply the transport already knows, for another
 * method, a foreign origin, an unknown protocol version, unreadable JSON, or a notification.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
sealed interface Parsed permits McpCall, Reply {}
