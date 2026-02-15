package com.druvu.web.php;

import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;
import com.druvu.web.api.plugin.TemplateEnginePlugin;

/**
 * Factory for creating PhpTemplateEnginePlugin instances.
 * Integrates with druvu-lib-loader for plugin discovery.
 *
 * @author Deniss Larka
 */
public class PhpTemplateEnginePluginFactory implements ComponentFactory<TemplateEnginePlugin> {

	@Override
	public TemplateEnginePlugin createComponent(Dependencies dependencies) {
		return new PhpTemplateEnginePlugin();
	}

	@Override
	public Class<TemplateEnginePlugin> getComponentType() {
		return TemplateEnginePlugin.class;
	}
}
