package com.tickets.application.usecase;

import com.tickets.domain.model.Ticket;
import com.tickets.domain.ports.NotificationService;
import com.tickets.domain.ports.TicketRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateTicketUseCase {

    private final TicketRepository repository;
    private final NotificationService notificationService;

    public CreateTicketUseCase(TicketRepository repository,
                               NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public Ticket createTicket(Long id, String descripcion) {

        Ticket ticket = new Ticket(id, descripcion, "ABIERTO");

        repository.save(ticket);

        notificationService.sendNotification(
                "Ticket creado con ID " + id
        );

        return ticket;
    }
}