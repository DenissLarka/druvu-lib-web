package com.druvu.web.api.handlers;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 *
 *
 * @author Deniss Larka
 * on 11 March 2022
 */
@Slf4j
public class ParamInfo {
	private final Map<String, String> params;

	public ParamInfo(Map<String, String[]> parameterMap) {
		this.params =
				parameterMap
						.entrySet()
						.stream()
						.filter(stringEntry -> stringEntry.getValue().length > 0)
						.filter(stringEntry -> !stringEntry.getValue()[0].trim().isEmpty())
						.collect(Collectors.toMap(Map.Entry::getKey, stringEntry -> stringEntry.getValue()[0]));
	}

	public String get(String key) {
		return params.get(key);
	}

	public Optional<String> getOptional(String key) {
		return Optional.ofNullable(params.get(key));
	}

	public String getOrDefault(String key, String defaultValue) {
		return params.getOrDefault(key, defaultValue);
	}

	public <R> Optional<R> getOptional(String key, Function<String, R> converter) {
		final String string = get(key);
		try {
			return Optional.ofNullable(converter.apply(string));
		}
		catch (Exception e) {
			//ignore
			log.debug(e.getMessage(), e);
		}
		return Optional.empty();
	}

	public <R> R get(String key, Function<String, R> converter) {
		return converter.apply(get(key));
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", "[", "]")
				.add("params=" + params)
				.toString();
	}
}
