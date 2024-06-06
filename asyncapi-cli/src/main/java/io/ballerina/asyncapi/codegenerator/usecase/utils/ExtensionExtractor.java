package io.ballerina.asyncapi.codegenerator.usecase.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ExtensionExtractor {
    public static Map<String, JsonNode> getExtensions(Object object) {
        try {
            Class<?> cls = object.getClass();
            Method method = cls.getMethod("getExtensions");
            Object map = method.invoke(object);
            if (map instanceof Map) {
                return (Map<String, JsonNode>) map;
            }
            throw new ClassCastException();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassCastException e) {
            return new HashMap<>();
        }
    }
}
