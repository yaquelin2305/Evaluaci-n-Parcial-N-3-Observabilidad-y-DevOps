package com.tickets.entrypoints.controller;

import com.tickets.application.usecase.CreateTicketUseCase;
import com.tickets.application.usecase.GetTicketUseCase;
import com.tickets.domain.model.Ticket;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Debemos declarar como @MockBean TODOS los casos de uso que inyecta el controlador
    @MockBean
    private CreateTicketUseCase createTicketUseCase;

    @MockBean
    private GetTicketUseCase getTicketUseCase;

    @Test
    void testEndpointCrearTicketDeberiaRetornarStatusOkYElTicketCreado() throws Exception {
        // 1. Configurar el comportamiento simulado (Mock) del caso de uso
        Ticket ticketSimulado = new Ticket();
        ticketSimulado.setId(10L);
        ticketSimulado.setDescripcion("Problema con la pasarela de pagos");
        ticketSimulado.setEstado("ABIERTO");

        Mockito.when(createTicketUseCase.createTicket(anyLong(), anyString())).thenReturn(ticketSimulado);

        // 2. Construir el JSON de entrada que espera el @RequestBody
        String ticketJsonInput = "{\"id\": 10, \"descripcion\": \"Problema con la pasarela de pagos\"}";

        // 3. Simular la petición HTTP POST enviando el JSON
        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJsonInput))
                .andExpect(status().isOk()) // Valida HTTP 200 OK según tu firma actual
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.descripcion").value("Problema con la pasarela de pagos"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"));
    }
}