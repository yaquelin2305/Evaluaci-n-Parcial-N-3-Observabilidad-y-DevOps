package com.tickets.application.usecase;

import com.tickets.domain.model.Ticket;
import com.tickets.domain.ports.NotificationService;
import com.tickets.domain.ports.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateTicketUseCaseTest {

    @Mock
    private TicketRepository repository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CreateTicketUseCase createTicketUseCase;

    @Test
    void debeCrearTicketConEstadoAbierto() {
        Long id = 1L;
        String descripcion = "Fallo en el sistema de pagos";

        Ticket result = createTicketUseCase.createTicket(id, descripcion);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(descripcion, result.getDescripcion());
        assertEquals("ABIERTO", result.getEstado());
    }

    @Test
    void debeGuardarTicketYNotificar() {
        Long id = 1L;
        String descripcion = "Fallo en el sistema de pagos";

        createTicketUseCase.createTicket(id, descripcion);

        verify(repository, times(1)).save(any(Ticket.class));
        verify(notificationService, times(1)).sendNotification("Ticket creado con ID " + id);
    }
}
