package com.druvu.web.example.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.druvu.web.api.handlers.HttpCall;
import com.druvu.web.api.handlers.HttpHandler;
import com.druvu.web.api.handlers.HttpRequest;
import com.druvu.web.api.handlers.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Deniss Larka
 * on 04 Aug 2022
 */
public class ExampleJsonHandler implements HttpHandler {

	private final static Gson GSON = new GsonBuilder()
			.serializeNulls()
			.setPrettyPrinting()
			.create();
	public static final Random RANDOM = new Random();

	@Override
	public void handle(HttpRequest request, HttpResponse response) {
		Map<String, String> object = new HashMap<>();
		object.put("attr1", "value1");
		object.put("attr2", "value2");
		object.put("attr3", String.format("Am I nice backend? %n%s %n", someText()));
		response.commitContent("application/json", GSON.toJson(object));
	}

	private String someText() {
		return RANDOM.nextBoolean() ? "Yes I'm!" : "Not so sure :(";
	}

}
