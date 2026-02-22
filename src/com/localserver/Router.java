package com.localserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Router {
    private final Map<String, Object> config;
    private final String root;

    public Router(Map<String, Object> config) {
        this.config = config;
        this.root = (String) config.getOrDefault("root", "./www");
    }

    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();
        String path = request.getPath();
        
        // Very basic routing logic:
        // 1. Check if it matches a defined route (e.g. /cgi-bin)
        // 2. Check if it's a file on disk
        
        Path rootPath = Paths.get(root).toAbsolutePath().normalize();
        Path filePath = rootPath.resolve(path.startsWith("/") ? path.substring(1) : path).normalize();
        
        if (!filePath.startsWith(rootPath)) {
            // Path traversal attempt?
            return error(403, "Forbidden");
        }
        
        File file = filePath.toFile();
        
        if (filePath.toString().endsWith(".py")) { // Simple check for CGI extension
             return handleCGI(request, filePath);
        }

        if (path.equals("/session")) {
            return handleSession(request);
        }
        
        if (request.getMethod().equalsIgnoreCase("PUT") && path.startsWith("/upload")) {
            return handleUpload(request);
        }

        if (file.exists()) {
            if (file.isDirectory()) {
                File indexFile = new File(file, "index.html");
                if (indexFile.exists()) {
                     return serveFile(indexFile);
                } else {
                    // Directory listing (if enabled)
                    // Simplified: return 403 or implement listing
                    return error(403, "Directory listing not implemented");
                }
            } else {
                return serveFile(file);
            }
        } else {
            return error(404, "Not Found");
        }
    }

    private HttpResponse handleSession(HttpRequest request) {
        String sessionId = request.getCookie("SESSIONID");
        Map<String, Object> session = null;
        boolean newSession = false;

        if (sessionId != null) {
            session = SessionManager.getSession(sessionId);
        }

        if (session == null) {
            sessionId = SessionManager.createSession();
            session = SessionManager.getSession(sessionId);
            newSession = true;
        }

        Integer visits = (Integer) session.getOrDefault("visits", 0);
        visits++;
        session.put("visits", visits);

        HttpResponse response = new HttpResponse();
        response.setStatus(200, "OK");
        response.setBody("<html><h1>Session Demo</h1><p>Session ID: " + sessionId + "</p><p>Visits: " + visits + "</p></html>", "text/html");
        
        if (newSession) {
            response.setCookie("SESSIONID", sessionId);
        }
        
        return response;
    }

    private HttpResponse handleUpload(HttpRequest request) {
        String uploadPath = "./www/uploads"; // Should come from config
        // Basic config lookup for upload path
        // Simplified: hardcoded or extracted from config map
        
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();
        
        String fileName = new File(request.getPath()).getName();
        File file = new File(uploadDir, fileName);
        
        try {
            // Write body to file
            // Note: Body in HttpRequest is String. For binary files this corrupts data!
            // HttpRequest needs to support byte[] body.
            // For now, only text upload works or we fix HttpRequest.
            Files.write(file.toPath(), request.getBody().getBytes());
            
            HttpResponse response = new HttpResponse();
            response.setStatus(201, "Created");
            response.setBody("File uploaded: " + fileName, "text/plain");
            return response;
        } catch (IOException e) {
             return error(500, "Upload failed");
        }
    }

    private HttpResponse serveFile(File file) {
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            HttpResponse response = new HttpResponse();
            response.setStatus(200, "OK");
            String mimeType = "application/octet-stream";
            String name = file.getName();
            if (name.endsWith(".html")) mimeType = "text/html";
            else if (name.endsWith(".css")) mimeType = "text/css";
            else if (name.endsWith(".js")) mimeType = "application/javascript";
            else if (name.endsWith(".png")) mimeType = "image/png";
            else if (name.endsWith(".jpg")) mimeType = "image/jpeg";
            
            response.setBody(content, mimeType);
            return response;
        } catch (IOException e) {
             return error(500, "Internal Server Error");
        }
    }

    private HttpResponse handleCGI(HttpRequest request, Path scriptPath) {
        return new CGIHandler().execute(request, scriptPath);
    }

    private HttpResponse error(int code, String message) {
        HttpResponse response = new HttpResponse();
        response.setStatus(code, message);
        response.setBody("<html><h1>" + code + " " + message + "</h1></html>", "text/html");
        return response;
    }
}
