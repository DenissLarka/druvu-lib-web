open module com.druvu.web.example {
	requires com.druvu.lib.web.core;
	requires com.druvu.lib.web.api;
	requires druvu.lib.loader;
	requires com.google.gson;
	requires org.slf4j;
	requires static lombok;
	requires static com.github.spotbugs.annotations;

	uses com.druvu.lib.loader.ComponentFactory;
}
