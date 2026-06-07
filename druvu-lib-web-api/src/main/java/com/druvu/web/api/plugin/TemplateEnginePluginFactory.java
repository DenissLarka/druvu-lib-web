package com.druvu.web.api.plugin;

import com.druvu.lib.loader.ComponentFactory;

/**
 * Base interface for template engine plugin factories.
 * Implementations provide this to integrate with druvu-lib-loader.
 *
 * @author Deniss Larka
 */
public interface TemplateEnginePluginFactory extends ComponentFactory<TemplateEnginePlugin> {

	@Override
	default Class<TemplateEnginePlugin> type() {
		return TemplateEnginePlugin.class;
	}
}
