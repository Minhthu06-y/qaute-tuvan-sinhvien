package vn.iotstar.service;

import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final SimpMessagingTemplate messaging;
    public NotificationService(SimpMessagingTemplate messaging) { this.messaging = messaging; }
    public void notifyUser(Long userId, String type, String message, Long questionId) {
        Object payload = Map.of("type", type, "message", message, "questionId", questionId);
        messaging.convertAndSend("/topic/notifications/" + userId, payload);
    }
    public void notifyManagers(Long facultyId, String type, String message, Long questionId) {
        Object payload = Map.of("type", type, "message", message, "questionId", questionId);
        messaging.convertAndSend("/topic/faculties/" + facultyId, payload);
    }
}
