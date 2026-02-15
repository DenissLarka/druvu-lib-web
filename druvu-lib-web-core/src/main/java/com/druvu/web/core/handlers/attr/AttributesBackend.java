package com.druvu.web.core.handlers.attr;

import java.util.Objects;

import jakarta.servlet.ServletContext;

/**
 * @author : Deniss Larka
 * on 09 June 2024
 **/
public interface AttributesBackend {

	Object getAttribute(String key);

	class ServletContextBackend implements AttributesBackend {

		private final ServletContext context;

		ServletContextBackend(ServletContext context) {
			this.context = Objects.requireNonNull(context);
		}

		@Override
		public Object getAttribute(String key) {
			return context.getAttribute(key);
		}
	}
}
