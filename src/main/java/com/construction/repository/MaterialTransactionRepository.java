package com.construction.repository;

import com.construction.entity.MaterialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaterialTransactionRepository extends JpaRepository<MaterialTransaction, UUID> {
    Page<MaterialTransaction> findByProject_ProjectCodeOrderByTransactionDateDesc(String projectCode, Pageable pageable);
}
