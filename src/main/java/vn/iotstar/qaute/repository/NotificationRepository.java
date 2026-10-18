package vn.iotstar.qaute.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.iotstar.qaute.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
        select n from Notification n
        left join n.receiver receiver
        left join n.recipient recipient
        where receiver.id = :receiverId or recipient.id = :receiverId
        order by n.createdAt desc
        """)
    List<Notification> findByReceiver_IdOrderByCreatedAtDesc(
        @Param("receiverId") Long receiverId
    );

    @Query("""
        select n from Notification n
        where n.receiver is null and n.recipient is null
        order by n.createdAt desc
        """)
    List<Notification> findByReceiverIsNullOrderByCreatedAtDesc();
}

