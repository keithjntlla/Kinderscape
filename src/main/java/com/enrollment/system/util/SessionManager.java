package com.enrollment.system.util;

import com.enrollment.system.dto.UserDto;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Component
public class SessionManager {
    
    private static SessionManager instance;
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private static final int SESSION_TIMEOUT_MINUTES = 120; // 2 hours
    
    public SessionManager() {
        instance = this;
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public String createSession(UserDto user) {
        String sessionToken = UUID.randomUUID().toString();
        Session session = new Session(user, LocalDateTime.now());
        activeSessions.put(sessionToken, session);
        return sessionToken;
    }
    
    public UserDto getUser(String sessionToken) {
        Session session = activeSessions.get(sessionToken);
        if (session != null && isSessionValid(session)) {
            session.updateLastActivity();
            return session.getUser();
        }
        return null;
    }
    
    public boolean isValidSession(String sessionToken) {
        Session session = activeSessions.get(sessionToken);
        return session != null && isSessionValid(session);
    }
    
    private boolean isSessionValid(Session session) {
        LocalDateTime timeout = session.getLastActivity().plusMinutes(SESSION_TIMEOUT_MINUTES);
        return LocalDateTime.now().isBefore(timeout);
    }
    
    public void invalidateSession(String sessionToken) {
        activeSessions.remove(sessionToken);
    }
    
    public void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> !isSessionValid(entry.getValue()));
    }
    
    // Inner class for session data
    private static class Session {
        private final UserDto user;
        private LocalDateTime lastActivity;
        
        public Session(UserDto user, LocalDateTime lastActivity) {
            this.user = user;
            this.lastActivity = lastActivity;
        }
        
        public UserDto getUser() {
            return user;
        }
        
        public LocalDateTime getLastActivity() {
            return lastActivity;
        }
        
        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now();
        }
    }
}