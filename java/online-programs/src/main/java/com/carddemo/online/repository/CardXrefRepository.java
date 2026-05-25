package com.carddemo.online.repository;

import com.carddemo.online.entity.CardXrefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardXrefRepository extends JpaRepository<CardXrefEntity, String> {
    Optional<CardXrefEntity> findByAccountId(String accountId);
}
