package com.tickets.infrastructure.adapter;

import org.springframework.stereotype.Component;

import com.tickets.domain.ports.NotificationService;

@Component
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendNotification(String message) {

        System.out.println("[PRODUCCION - ALERTA] Notificación procesada: " + message);

    }
}
