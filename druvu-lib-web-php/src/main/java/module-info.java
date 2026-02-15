module druvu.lib.web.php {
	requires static lombok;
	requires static com.github.spotbugs.annotations;

	requires druvu.lib.web.api;
	requires druvu.lib.loader;
	requires org.eclipse.jetty.ee10.servlet;
	requires org.slf4j;

	// Export plugin API
	exports com.druvu.web.php;

	// Open internal packages for Jetty reflection
	opens com.druvu.web.php.internal to org.eclipse.jetty.ee10.servlet;
	opens com.druvu.web.php.internal.func to org.eclipse.jetty.ee10.servlet;

	// Register a plugin factory
	provides com.druvu.lib.loader.ComponentFactory
		with com.druvu.web.php.PhpTemplateEnginePluginFactory;
}
