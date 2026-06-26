package com.tickets.infrastructure.adapter;

import com.tickets.domain.model.Ticket;
import com.tickets.domain.ports.TicketRepository;
import com.tickets.infrastructure.entity.TicketEntity;
import com.tickets.infrastructure.repository.JpaTicketRepository;
import org.springframework.stereotype.Component;

@Component
public class TicketRepositoryImpl implements TicketRepository {

    private final JpaTicketRepository repository;

    public TicketRepositoryImpl(JpaTicketRepository repository) {
        this.repository = repository;
    }

    @Override
    public Ticket save(Ticket ticket) {

        TicketEntity entity = new TicketEntity(
                ticket.getId(),
                ticket.getDescripcion(),
                ticket.getEstado()
        );

        TicketEntity saved = repository.save(entity);

        return new Ticket(
                saved.getId(),
                saved.getDescripcion(),
                saved.getEstado()
        );
    }

    @Override
    public Ticket findById(Long id) {

        TicketEntity entity = repository.findById(id).orElse(null);

        if (entity == null) return null;

        return new Ticket(
                entity.getId(),
                entity.getDescripcion(),
                entity.getEstado()
        );
    }
}