package com.snovasys.ledgerline.repository;

import com.snovasys.ledgerline.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findAllByOrderByCreatedAtDesc();
    List<AuditEvent> findAllByActorIdOrderByCreatedAtDesc(Long actorId);
}
