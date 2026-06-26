package com.tickets.application.usecase;

import com.tickets.domain.model.Ticket;
import com.tickets.domain.ports.TicketRepository;
import org.springframework.stereotype.Service;

@Service
public class GetTicketUseCase {

    private final TicketRepository repository;

    public GetTicketUseCase(TicketRepository repository) {
        this.repository = repository;
    }

    public Ticket getTicket(Long id) {
        return repository.findById(id);
    }
}