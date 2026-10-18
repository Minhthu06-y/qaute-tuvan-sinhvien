package vn.iotstar.qaute.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.iotstar.qaute.entity.Notification;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
        findByRecipientIsNullOrderByCreatedAtDesc();

    List<Notification>
        findByRecipient_IdOrderByCreatedAtDesc(Long userId);
}