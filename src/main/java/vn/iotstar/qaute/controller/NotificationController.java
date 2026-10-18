package vn.iotstar.qaute.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.iotstar.qaute.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/announcement")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendAnnouncement(
            @RequestBody AnnouncementRequest request) {

        if (request.title() == null
                || request.title().isBlank()
                || request.content() == null
                || request.content().isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Tiêu đề và nội dung là bắt buộc.")
            );
        }

        if (request.title().length() > 200
                || request.content().length() > 2000) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Nội dung thông báo quá dài.")
            );
        }

        notificationService.sendAnnouncement(
            request.title(), request.content()
        );

        return ResponseEntity.ok(
            Map.of("message", "Đã gửi thông báo.")
        );
    }

    public record AnnouncementRequest(
        String title,
        String content
    ) {
    }
}