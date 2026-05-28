package com.tickets.application.usecase;

import com.tickets.domain.model.Ticket;
import com.tickets.domain.ports.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetTicketUseCaseTest {

    @Mock
    private TicketRepository repository;

    @InjectMocks
    private GetTicketUseCase getTicketUseCase;

    @Test
    void debeRetornarTicketCuandoExiste() {
        Ticket expected = new Ticket(1L, "Error en login", "ABIERTO");
        when(repository.findById(1L)).thenReturn(expected);

        Ticket result = getTicketUseCase.getTicket(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Error en login", result.getDescripcion());
        assertEquals("ABIERTO", result.getEstado());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void debeRetornarNullCuandoNoExiste() {
        when(repository.findById(99L)).thenReturn(null);

        Ticket result = getTicketUseCase.getTicket(99L);

        assertNull(result);
        verify(repository, times(1)).findById(99L);
    }
}
