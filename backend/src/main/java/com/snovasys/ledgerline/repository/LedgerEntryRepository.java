package com.snovasys.ledgerline.repository;

import com.snovasys.ledgerline.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findAllByTransactionId(UUID transactionId);

    List<LedgerEntry> findAllByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Query("SELECT COALESCE(SUM(l.amountPaise), 0) FROM LedgerEntry l WHERE l.entryType = :entryType")
    Long sumAmountByEntryType(@Param("entryType") String entryType);

    @Query("SELECT l FROM LedgerEntry l ORDER BY l.createdAt DESC")
    List<LedgerEntry> findAllOrderByCreatedAtDesc();
}
