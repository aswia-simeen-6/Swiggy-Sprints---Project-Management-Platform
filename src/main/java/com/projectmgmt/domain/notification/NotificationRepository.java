package com.projectmgmt.domain.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    Slice<Notification> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.read = false ORDER BY n.createdAt DESC")
    Slice<Notification> findUnreadByUserId(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.read = false")
    long countUnreadByUserId(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsRead(UUID userId);
}
