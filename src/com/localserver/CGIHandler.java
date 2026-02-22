package com.localserver;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class CGIHandler {
    public HttpResponse execute(HttpRequest request, Path scriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(scriptPath.toAbsolutePath().toString());
            Map<String, String> env = pb.environment();
            
            // Set CGI environment variables
            env.put("REQUEST_METHOD", request.getMethod());
            String query = request.getQueryString();
            env.put("QUERY_STRING", query != null ? query : "");
            env.put("PATH_INFO", request.getPath());
            env.put("SERVER_PROTOCOL", "HTTP/1.1");
            env.put("REMOTE_ADDR", "127.0.0.1"); // Should get from socket
            
            // Start process
            pb.redirectErrorStream(true); // Merge stderr into stdout
            Process process = pb.start();
            
            // Write body to process input stream (for POST)
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(request.getBody().getBytes());
                    os.flush();
                }
            }
            
            // Read output
            InputStream is = process.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            String output = new String(buffer.toByteArray());
            // System.out.println("CGI Output: [" + output + "]");
            
            HttpResponse response = new HttpResponse();
            
            // Simple parsing of CGI output
            String delimiter = "\r\n\r\n";
            int bodyStart = output.indexOf(delimiter);
            if (bodyStart == -1) {
                delimiter = "\n\n";
                bodyStart = output.indexOf(delimiter);
            }
            // System.out.println("BodyStart: " + bodyStart);

            if (bodyStart != -1) {
                String headersPart = output.substring(0, bodyStart);
                String bodyPart = output.substring(bodyStart + delimiter.length());
                
                String[] lines = headersPart.split("\n");

                String contentType = "text/html"; // Default for CGI? Or plain?
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    int colon = line.indexOf(":");
                    if (colon != -1) {
                         String key = line.substring(0, colon).trim();
                         String value = line.substring(colon + 1).trim();
                         if (key.equalsIgnoreCase("Status")) {
                             // Status: 200 OK
                             String[] statusParts = value.split(" ", 2);
                             if (statusParts.length >= 2) {
                                 response.setStatus(Integer.parseInt(statusParts[0]), statusParts[1]);
                             }
                         } else {
                             response.setHeader(key, value);
                             if (key.equalsIgnoreCase("Content-Type")) {
                                 contentType = value;
                             }
                         }
                    }
                }
                response.setBody(bodyPart, contentType); 
            } else {
                response.setBody(output);
            }
            
            return response;
            
        } catch (IOException e) {
            e.printStackTrace();
            HttpResponse err = new HttpResponse();
            err.setStatus(500, "CGI Error");
            err.setBody("CGI execution failed: " + e.getMessage());
            return err;
        }
    }
}
