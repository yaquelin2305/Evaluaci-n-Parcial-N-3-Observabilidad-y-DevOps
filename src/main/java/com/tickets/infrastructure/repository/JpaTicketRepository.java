package com.tickets.infrastructure.repository;

import com.tickets.infrastructure.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTicketRepository extends JpaRepository<TicketEntity, Long> {
}