package com.localserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ConfigLoader {
    private String jsonContent;
    private int index;

    public ConfigLoader(String filePath) throws IOException {
        this.jsonContent = new String(Files.readAllBytes(Paths.get(filePath))).trim();
        this.index = 0;
    }

    public Map<String, Object> parse() {
        skipWhitespace();
        if (jsonContent.charAt(index) == '{') {
            return parseObject();
        } else {
            throw new RuntimeException("Config must start with an object");
        }
    }

    private Map<String, Object> parseObject() {
        Map<String, Object> map = new HashMap<>();
        consume('{');
        skipWhitespace();
        while (jsonContent.charAt(index) != '}') {
            String key = parseString();
            skipWhitespace();
            consume(':');
            skipWhitespace();
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();
            if (jsonContent.charAt(index) == ',') {
                consume(',');
                skipWhitespace();
            }
        }
        consume('}');
        return map;
    }

    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        consume('[');
        skipWhitespace();
        while (jsonContent.charAt(index) != ']') {
            Object value = parseValue();
            list.add(value);
            skipWhitespace();
            if (jsonContent.charAt(index) == ',') {
                consume(',');
                skipWhitespace();
            }
        }
        consume(']');
        return list;
    }

    private Object parseValue() {
        char c = jsonContent.charAt(index);
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (Character.isDigit(c) || c == '-') return parseNumber();
        if (jsonContent.startsWith("true", index)) {
            index += 4;
            return true;
        }
        if (jsonContent.startsWith("false", index)) {
            index += 5;
            return false;
        }
        if (jsonContent.startsWith("null", index)) {
            index += 4;
            return null;
        }
        throw new RuntimeException("Unexpected character at " + index + ": " + c);
    }

    private String parseString() {
        consume('"');
        StringBuilder sb = new StringBuilder();
        while (jsonContent.charAt(index) != '"') {
            // Very basic escape handling
            if (jsonContent.charAt(index) == '\\') {
                index++;
            }
            sb.append(jsonContent.charAt(index));
            index++;
        }
        consume('"');
        return sb.toString();
    }

    private Number parseNumber() {
        int start = index;
        while (index < jsonContent.length() && (Character.isDigit(jsonContent.charAt(index)) || jsonContent.charAt(index) == '.' || jsonContent.charAt(index) == '-')) {
            index++;
        }
        String numStr = jsonContent.substring(start, index);
        if (numStr.contains(".")) {
            return Double.parseDouble(numStr);
        }
        return Integer.parseInt(numStr);
    }

    private void skipWhitespace() {
        while (index < jsonContent.length() && Character.isWhitespace(jsonContent.charAt(index))) {
            index++;
        }
    }

    private void consume(char c) {
        if (jsonContent.charAt(index) != c) {
            throw new RuntimeException("Expected '" + c + "' but found '" + jsonContent.charAt(index) + "' at index " + index);
        }
        index++;
    }
}
