package vn.iotstar.qaute.service;

import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.qaute.entity.Notification;
import vn.iotstar.qaute.entity.User;
import vn.iotstar.qaute.repository.NotificationRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
        NotificationRepository notificationRepository,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void notify(User receiver, String message, String link) {
        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setMessage(message);
        notification.setLink(link);
        notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
            receiver.getEmail(),
            "/queue/notifications",
            Map.of(
                "id", notification.getId(),
                "message", message,
                "link", link == null ? "" : link
            )
        );
    }

    @Transactional
    public void sendAnnouncement(String title, String content) {
        Notification notification = new Notification();
        notification.setTitle(title.trim());
        notification.setContent(content.trim());
        notificationRepository.save(notification);

        messagingTemplate.convertAndSend(
            "/topic/announcements",
            new AnnouncementMessage(title.trim(), content.trim())
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getAnnouncements() {
        return notificationRepository.findByReceiverIsNullOrderByCreatedAtDesc();
    }

    public record AnnouncementMessage(String title, String content) {
    }
}

