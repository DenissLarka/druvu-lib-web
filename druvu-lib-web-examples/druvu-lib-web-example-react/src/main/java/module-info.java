/**
 * A React app on druvu-lib-web: core and api only, no template engine. Open, so the core may instantiate the handlers.
 */
open module com.druvu.web.example.react {
    requires com.druvu.lib.web.core;
    requires com.druvu.lib.web.api;
    requires org.slf4j;
}
