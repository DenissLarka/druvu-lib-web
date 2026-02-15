package com.druvu.web.api.handlers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Deniss Larka
 * 13 March 2020
 */
@Slf4j
public final class PathInfo {

	public static final String EMPTY = "";
	private final List<String> list = new ArrayList<>();
	private final String contextPath;

	private PathInfo() {
		this.contextPath = null;
	}

	public PathInfo(String contextPath, String pathInfo) {
		this(pathInfo, contextPath, null);
	}

	public PathInfo(String contextPath, String pathInfo, String defaultPath) {
		this.contextPath = contextPath;
		final Path path = Paths.get(nonNullPathInfo(pathInfo)).normalize();
		for (int i = 0; i < path.getNameCount(); i++) {
			String part = path.getName(i).toString().trim();
			if (!part.isEmpty()) {
				list.add(part.toLowerCase(Locale.ENGLISH));
			}
		}
		if (path.getNameCount() == 0 && defaultPath != null) {
			list.add(defaultPath);
		}
	}

	private String nonNullPathInfo(String pathInfo) {
		return pathInfo != null ? pathInfo : EMPTY;
	}

	public String getOrDefault(int index, String defaultValue) {
		if (index < 0 || index >= list.size()) {
			return defaultValue;
		}
		return list.get(index);
	}

	public Optional<String> pathOpt(int index) {
		return Optional.ofNullable(getOrDefault(index, null));
	}

	public <R> Optional<R> pathOpt(int index, Function<String, R> converter) {
		return pathParamOpt(index, converter);
	}

	public String contextPath() {
		return contextPath;
	}

	public String withContextPath(String target) {
		return contextPath + '/' + target;
	}

	/*
	 * "mainPath" is the first path after the context.
	 * http://example.com/context-path/main-path/path-param-1/path-param-2
	 * if we are at the root, "/" after context,
	 * it should be a "defaultPath" provided through config
	 */
	public String mainPath() {
		return getOrDefault(0, EMPTY);
	}

	public boolean isFirstPath(String toCheck) {
		return mainPath().equalsIgnoreCase(toCheck);
	}

	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return mainPath().isEmpty();
	}

	public <R> Optional<R> pathParamOpt(int index, Function<String, R> converter) {
		final String string = getOrDefault(index, null);
		try {
			return Optional.ofNullable(converter.apply(string));
		}
		catch (Exception e) {
			//ignore
			log.debug(e.getMessage(), e);
		}
		return Optional.empty();
	}

	public <R> R pathParam(int index, Function<String, R> converter) {
		return converter.apply(getOrDefault(index, null));
	}

	@Override
	public String toString() {
		return String.valueOf(list);
	}

}
