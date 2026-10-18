package vn.iotstar.qaute.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.qaute.entity.Notification;
import vn.iotstar.qaute.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void sendAnnouncement(String title, String content) {
        Notification notification = new Notification();
        notification.setTitle(title.trim());
        notification.setContent(content.trim());

        // Lưu thông báo chung vào database.
        notificationRepository.save(notification);

        // Gửi tới các client đang subscribe topic.
        messagingTemplate.convertAndSend(
            "/topic/announcements",
            new AnnouncementMessage(
                title.trim(),
                content.trim()
            )
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> getAnnouncements() {
        return notificationRepository
            .findByRecipientIsNullOrderByCreatedAtDesc();
    }

    public record AnnouncementMessage(
        String title,
        String content
    ) {
    }
}