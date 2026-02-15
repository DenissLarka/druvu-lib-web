package com.druvu.web.core.utils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.druvu.web.api.handlers.ParamInfo;

/**
 * @author Deniss Larka
 * <br/>on 21 Apr 2022
 */
public final class ParamValidator {

	public static final String PARAMETER_EXISTING_ALERT = "Parameter %s must be provided";
	private final ParamInfo params;
	private final Consumer<String> errors;
	private boolean allValid = true;

	private ParamValidator(ParamInfo params, Consumer<String> errors) {
		this.params = Objects.requireNonNull(params);
		this.errors = Objects.requireNonNull(errors);
	}

	public static ParamValidator from(ParamInfo params, Consumer<String> errors) {
		return new ParamValidator(params, errors);
	}

	public ParamValidator validateExist(String key) {
		final String value = params.get(key);
		if (value == null) {
			errors.accept(String.format(PARAMETER_EXISTING_ALERT, key));
			allValid = false;
		}
		return this;
	}

	public ParamValidator validateCustom(String key, Predicate<String> tryCall, String expectedPattern) {
		final String value = params.get(key);
		if (value == null) {
			errors.accept(String.format(PARAMETER_EXISTING_ALERT, key));
			allValid = false;
		} else {
			try {
				if (!tryCall.test(value)) {
					throw new IllegalArgumentException();
				}
			}
			catch (Throwable e) { //NOPMD
				errors.accept(String.format("Parameter %s must match the pattern:%s", key, expectedPattern));
				allValid = false;
			}
		}
		return this;
	}

	public ParamValidator validateDate(String key, String pattern) {
		return validateCustom(key, value -> DateTimeFormatter.ofPattern(pattern) != null, pattern);
	}

	public ParamValidator validateInt(String key) {
		final String value = params.get(key);
		if (value == null) {
			errors.accept(String.format(PARAMETER_EXISTING_ALERT, key));
			allValid = false;
		} else {
			try {
				Integer.parseInt(params.get(key));
			}
			catch (NumberFormatException e) {
				errors.accept(String.format("Parameter %s must be an integer", key));
				allValid = false;
			}
		}
		return this;
	}

	public ParamValidator validateDecimal(String key) {
		final String value = params.get(key);
		if (value == null) {
			errors.accept(String.format(PARAMETER_EXISTING_ALERT, key));
			allValid = false;
		} else {
			try {
				new BigDecimal(value);
			}
			catch (NumberFormatException e) {
				errors.accept(String.format("Parameter %s must be an integer", key));
				allValid = false;
			}
		}
		return this;

	}

	public boolean hasNoErrors() {
		return allValid;
	}

	public boolean hasErrors() {
		return !allValid;
	}
}
