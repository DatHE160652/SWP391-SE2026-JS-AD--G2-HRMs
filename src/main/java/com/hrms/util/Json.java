package com.hrms.util;

import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

public final class Json {
    private Json() { }

    public static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) return map.entrySet().stream()
                .map(entry -> ValidationSupport.quote(String.valueOf(entry.getKey())) + ":" + encode(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
        if (value instanceof Collection<?> collection) return collection.stream().map(Json::encode)
                .collect(Collectors.joining(",", "[", "]"));
        return ValidationSupport.quote(value.toString());
    }
}
