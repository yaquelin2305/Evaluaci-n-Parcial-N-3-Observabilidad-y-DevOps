package com.tickets.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicketTest {

    @Test
    void testPropiedadesDelModeloTicket() {
        // Instanciamos la entidad pura del dominio
        Ticket ticket = new Ticket();
        ticket.setId(99L);
        ticket.setDescripcion("Fallo crítico de red");
        ticket.setEstado("CERRADO");

        // Validamos sus Getters y Setters
        assertEquals(99L, ticket.getId());
        assertEquals("Fallo crítico de red", ticket.getDescripcion());
        assertEquals("CERRADO", ticket.getEstado());
    }
}