package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Transaction;
import fr.mossaab.security.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
