package vn.iotstar.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AdvisorPresenceService {
    private static final Duration ONLINE_WINDOW = Duration.ofSeconds(90);
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, Instant>> sessions = new ConcurrentHashMap<>();

    public void heartbeat(Long userId, String sessionId) {
        sessions.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>()).put(sessionId, Instant.now());
    }

    public void offline(Long userId, String sessionId) {
        ConcurrentHashMap<String, Instant> activeSessions = sessions.get(userId);
        if (activeSessions == null) return;
        activeSessions.remove(sessionId);
        if (activeSessions.isEmpty()) sessions.remove(userId, activeSessions);
    }

    public boolean isOnline(Long userId) {
        ConcurrentHashMap<String, Instant> activeSessions = sessions.get(userId);
        if (activeSessions == null) return false;
        Instant cutoff = Instant.now().minus(ONLINE_WINDOW);
        activeSessions.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        if (activeSessions.isEmpty()) {
            sessions.remove(userId, activeSessions);
            return false;
        }
        return true;
    }
}
