package com.localserver;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    private Map<String, String> queryParams = new HashMap<>();
    private Map<String, String> cookies = new HashMap<>();

    public HttpRequest(String rawRequest) {
        parse(rawRequest);
    }

    private void parse(String rawRequest) {
        // Split by double CRLF for headers vs body
        int splitIndex = rawRequest.indexOf("\r\n\r\n");
        if (splitIndex == -1) {
             // Fallback to \n\n
             splitIndex = rawRequest.indexOf("\n\n");
        }
        
        String headerPart;
        if (splitIndex != -1) {
            headerPart = rawRequest.substring(0, splitIndex);
            // +4 for \r\n\r\n, or +2 for \n\n. Let's just take substring after match
            this.body = rawRequest.substring(splitIndex + (rawRequest.charAt(splitIndex) == '\r' ? 4 : 2));
        } else {
            headerPart = rawRequest;
            this.body = "";
        }

        String[] lines = headerPart.split("\n"); // simple split by newline
        if (lines.length > 0) {
            String line0 = lines[0].trim();
            String[] requestLine = line0.split(" ");
            if (requestLine.length >= 3) {
                this.method = requestLine[0];
                this.path = requestLine[1];
                this.version = requestLine[2];
                parseQueryString();
            }
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
                
                if (key.equalsIgnoreCase("Cookie")) {
                    parseCookies(value);
                }
            }
        }
    }

    private void parseCookies(String cookieHeader) {
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                cookies.put(kv[0].trim(), kv[1].trim());
            }
        }
    }

    private void parseQueryString() {
        if (path.contains("?")) {
            String[] parts = path.split("\\?", 2);
            this.path = parts[0];
            String queryString = parts[1];
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    queryParams.put(kv[0], kv[1]);
                } else if (kv.length == 1) {
                    queryParams.put(kv[0], "");
                }
            }
        }
    }

    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getVersion() { return version; }
    public String getHeader(String key) { return headers.get(key); }
    public String getBody() { return body; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public String getQueryString() {
        if (path != null && path.contains("?")) {
            return path.split("\\?", 2)[1];
        }
        return null;
    }
    public String getCookie(String key) { return cookies.get(key); }

    @Override
    public String toString() {
        return method + " " + path + " " + version;
    }
}
