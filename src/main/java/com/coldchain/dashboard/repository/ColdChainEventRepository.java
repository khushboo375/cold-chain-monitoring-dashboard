package com.coldchain.dashboard.repository;

import com.coldchain.dashboard.model.ColdChainEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColdChainEventRepository extends JpaRepository<ColdChainEvent, Long> {
}
