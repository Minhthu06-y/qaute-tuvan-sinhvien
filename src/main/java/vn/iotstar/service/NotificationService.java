
package vn.iotstar.service;

import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.iotstar.entity.Notification;
import vn.iotstar.entity.User;
import vn.iotstar.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messaging;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messaging) {
        this.notificationRepository = notificationRepository;
        this.messaging = messaging;
    }

    /**
     * Lưu thông báo cá nhân và gửi qua WebSocket.
     */
    @Transactional
    public void notify(User receiver, String message, String link) {
        if (receiver == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setMessage(message);
        notification.setLink(link);

        notificationRepository.save(notification);

        messaging.convertAndSendToUser(
                receiver.getEmail(),
                "/queue/notifications",
                Map.of(
                        "id", notification.getId(),
                        "message", message,
                        "link", link == null ? "" : link
                )
        );
    }

    /**
     * Gửi thông báo đến một user theo ID.
     * Dùng cho các chức năng hiện có của nhánh vn.iotstar.service.
     */
    public void notifyUser(
            Long userId,
            String type,
            String message,
            Long questionId) {

    	Object payload = Map.<String, Object>of(
    		    "type", type,
    		    "message", message,
    		    "questionId", questionId == null ? "" : questionId
    	);

        messaging.convertAndSend(
                "/topic/notifications/" + userId,
                (Object) payload
        );
    }

    /**
     * Gửi thông báo đến nhóm quản lý theo khoa.
     */
    public void notifyManagers(
            Long facultyId,
            String type,
            String message,
            Long questionId) {

    	Object payload = Map.<String, Object>of(
    		    "type", type,
    		    "message", message,
    		    "questionId", questionId == null ? "" : questionId
    	);

        messaging.convertAndSend(
                "/topic/faculties/" + facultyId,
                (Object) payload
        );
    }

    /**
     * Gửi thông báo chung đến toàn bộ người đăng ký topic.
     */
    @Transactional
    public void sendAnnouncement(String title, String content) {
        Notification notification = new Notification();
        notification.setTitle(title.trim());
        notification.setContent(content.trim());

        notificationRepository.save(notification);

        messaging.convertAndSend(
                "/topic/announcements",
                new AnnouncementMessage(title.trim(), content.trim())
        );
    }

    /**
     * Lấy các thông báo chung chưa gắn với người nhận.
     */
    @Transactional(readOnly = true)
    public List<Notification> getAnnouncements() {
        return notificationRepository
                .findByReceiverIsNullOrderByCreatedAtDesc();
    }

    public record AnnouncementMessage(String title, String content) {
    }
}
