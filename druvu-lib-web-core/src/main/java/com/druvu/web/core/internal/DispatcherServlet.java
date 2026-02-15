package com.druvu.web.core.internal;

import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.PathInfo;
import com.druvu.web.api.config.UrlConfig;
import com.druvu.web.core.handlers.ErrorHandler;
import com.druvu.web.core.handlers.HttpCall;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import java.util.Optional;

/**
 * @author Deniss Larka
 * on 03 March 2019
 */
public class DispatcherServlet extends HttpServlet {

	private static final HttpHandler NOT_FOUND = new ErrorHandler();
	private static final long serialVersionUID = 1L;

	@Override
	public void init() {
		//init
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		handle(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		handle(req, resp);
	}

	@SneakyThrows
	private void handle(HttpServletRequest req, HttpServletResponse resp) {
		Optional<HttpCall> callOpt = HandlerUtils.process(req, resp);
		if (callOpt.isEmpty()) {
			resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}
		final HttpCall call = callOpt.get();

		PathInfo pathInfo = call.pathInfo();
		final String mainPath = pathInfo.mainPath();

		final String templateSystem = ContextVars.templateSystem(getServletContext());
		if (req.getDispatcherType() == DispatcherType.REQUEST) {
			final HttpHandler httpHandler = handler(mainPath);

			httpHandler.handle(call.request(), call.response());
			if (resp.isCommitted()) {
				return;
			}
			dispatcher(req, templateSystem).forward(new RequestWrapper(req, '/' + mainPath + "." + templateSystem), resp);
		}

		if (req.getDispatcherType() == DispatcherType.INCLUDE) {
			dispatcher(req, templateSystem).include(req, resp);
		}
	}

	@SneakyThrows
	private HttpHandler handler(String matchPath) {
		final UrlConfig urlConfig = ContextVars.handlers(getServletContext()).get(matchPath);
		if (urlConfig == null) {
			return NOT_FOUND;
		}
		final Class<HttpHandler> handlerClass = urlConfig.urlHandlerClass();
		return handlerClass.getDeclaredConstructor().newInstance();
	}

	private RequestDispatcher dispatcher(HttpServletRequest request, String dispatcherName) {
		final RequestDispatcher dispatcher = request.getServletContext().getNamedDispatcher(dispatcherName);
		if (dispatcher == null) {
			throw new IllegalStateException(dispatcherName + " dispatcher not found");
		}
		return dispatcher;
	}

}
