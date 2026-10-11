/**
 * An MCP server on druvu-lib-web: core, api and druvu-lib-json; no template engine. Open, so the core may instantiate
 * the handlers.
 */
open module com.druvu.web.example.mcp {
    requires com.druvu.lib.web.core;
    requires com.druvu.lib.web.api;
    requires com.druvu.json;
    requires org.slf4j;
    requires static com.github.spotbugs.annotations;
}
