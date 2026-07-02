package com.tickets.entrypoints.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tickets.application.usecase.CreateTicketUseCase;
import com.tickets.application.usecase.GetTicketUseCase;
import com.tickets.domain.model.Ticket;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final CreateTicketUseCase createTicketUseCase;
    private final GetTicketUseCase getTicketUseCase;

    public TicketController(CreateTicketUseCase createTicketUseCase,
                            GetTicketUseCase getTicketUseCase) {
        this.createTicketUseCase = createTicketUseCase;
        this.getTicketUseCase = getTicketUseCase;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Todo ok con los tickets";
    }

    @PostMapping
    public Ticket createTicket(@RequestBody Ticket ticket) {

        return createTicketUseCase.createTicket(
                ticket.getId(),
                ticket.getDescripcion()
        );
    }

    @GetMapping("/{id}")
    public Ticket getTicket(@PathVariable Long id) {

        return getTicketUseCase.getTicket(id);
    }
}