package com.snovasys.ledgerline.repository;

import com.snovasys.ledgerline.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionRef(String transactionRef);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId ORDER BY t.createdAt DESC")
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccount.user.id = :userId OR t.destinationAccount.user.id = :userId) ORDER BY t.createdAt DESC")
    List<Transaction> findAllByUserId(@Param("userId") Long userId);
}
