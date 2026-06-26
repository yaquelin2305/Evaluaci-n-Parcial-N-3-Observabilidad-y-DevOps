package com.tickets.domain.ports;

import com.tickets.domain.model.Ticket;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Ticket findById(Long id);
}