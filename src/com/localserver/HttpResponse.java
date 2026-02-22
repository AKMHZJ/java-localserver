package com.localserver;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private byte[] body = new byte[0];

    public HttpResponse() {
        headers.put("Server", "JavaNioServer/1.0");
        headers.put("Connection", "close");
    }

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
        setHeader("Content-Type", "text/plain; charset=utf-8");
        setHeader("Content-Length", String.valueOf(this.body.length));
    }
    
    public void setBody(String body, String contentType) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
        setHeader("Content-Type", contentType);
        setHeader("Content-Length", String.valueOf(this.body.length));
    }

    public void setBody(byte[] body, String contentType) {
        this.body = body;
        setHeader("Content-Type", contentType);
        setHeader("Content-Length", String.valueOf(this.body.length));
    }

    public void setCookie(String key, String value) {
        setHeader("Set-Cookie", key + "=" + value + "; Path=/; HttpOnly");
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        
        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] response = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, response, 0, headerBytes.length);
        System.arraycopy(body, 0, response, headerBytes.length, body.length);
        
        return response;
    }
}
