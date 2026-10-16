package vn.iotstar.qaute.service;

import vn.iotstar.qaute.entity.*;
import vn.iotstar.qaute.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repo;
    private final SimpMessagingTemplate ws;

    /** Lưu DB + đẩy realtime. Client subscribe: /user/queue/notifications */
    public void notify(User receiver, String message, String link) {
        Notification n = new Notification();
        n.setReceiver(receiver);
        n.setMessage(message);
        n.setLink(link);
        repo.save(n);
        ws.convertAndSendToUser(receiver.getEmail(), "/queue/notifications",
                Map.of("id", n.getId(), "message", message, "link", link == null ? "" : link));
    }
}

