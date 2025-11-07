package com.unimag.bustransport.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

 //Factory para obtener la implementación correcta de NotificationService

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFactory {

    private final List<NotificationService> notificationServices;

    private final Map<NotificationType, NotificationService> servicesByType;

    @Autowired
    public NotificationFactory(List<NotificationService> notificationServices) {
        this.notificationServices = notificationServices;
        
        // Crear un mapa: NotificationType -> NotificationService
        this.servicesByType = notificationServices.stream()
                .collect(Collectors.toMap(
                        NotificationService::getType,
                        Function.identity()
                ));

    }
    

     //Obtiene el servicio de notificación apropiado según el tipo
    public NotificationService getService(NotificationType type) {
        NotificationService service = servicesByType.get(type);
        
        if (service == null) {
            throw new IllegalArgumentException(
                    String.format("No notification service found for type: %s. " +
                            "Available types: %s", type, servicesByType.keySet())
            );
        }
        
        if (!service.isEnabled()) {
            log.warn("Notification service {} is disabled", type);
            throw new IllegalStateException(
                    String.format("Notification service %s is currently disabled", type)
            );
        }
        
        return service;
    }
    

    public void send(NotificationRequest request) {
        NotificationService service = getService(request.type());
        service.send(request);
    }
    

    public boolean isAvailable(NotificationType type) {
        NotificationService service = servicesByType.get(type);
        return service != null && service.isEnabled();
    }

    public List<NotificationType> getAvailableTypes() {
        return servicesByType.entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}