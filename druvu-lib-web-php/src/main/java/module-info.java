module com.druvu.lib.web.php {
    requires static com.github.spotbugs.annotations;

    // The engine, which a user of this module may reach for directly
    requires transitive com.druvu.lib.php;
    requires com.druvu.lib.web.api;
    requires com.druvu.lib.loader;
    requires org.eclipse.jetty.ee10.servlet;
    requires org.slf4j;

    // Export plugin API
    exports com.druvu.web.php;

    // Open internal packages for Jetty reflection
    opens com.druvu.web.php.internal to
            org.eclipse.jetty.ee10.servlet;

    // Register a plugin factory
    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.web.php.PhpTemplateEnginePluginFactory;
}
