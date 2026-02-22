package com.localserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {
    private static final Map<String, Map<String, Object>> sessions = new HashMap<>();

    public static String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new HashMap<>());
        return sessionId;
    }

    public static Map<String, Object> getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public static void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
