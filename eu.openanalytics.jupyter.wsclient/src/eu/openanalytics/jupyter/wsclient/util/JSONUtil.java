/*******************************************************************************
 * Copyright (c) 2016 Open Analytics NV and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package eu.openanalytics.jupyter.wsclient.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JSONUtil {

	private static Gson gson = new Gson();
	
	public static Map<String, Object> toMap(String json) {
		Type type = new TypeToken<Map<String, Object>>(){}.getType();
		Map<String, Object> map = gson.fromJson(json, type);
		return map;
	}
	
	public static List<Map<String, Object>> toList(String json) {
		Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
		List<Map<String, Object>> list = gson.fromJson(json, type);
		return list;
	}
	
	public static String toJSON(String key, String value) {
		Map<String, String> map = new HashMap<>();
		map.put(key, value);
		return gson.toJson(map);
	}
	
	public static String toJSON(String[] keys, String[] values) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i < keys.length; i++) {
			map.put(keys[i], values[i]);
		}
		return gson.toJson(map);
	}
}
