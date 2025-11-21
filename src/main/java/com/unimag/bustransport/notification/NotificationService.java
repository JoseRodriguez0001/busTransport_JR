package com.unimag.bustransport.notification;

public interface NotificationService {

    void send(NotificationRequest request);
    NotificationType getType();
    default boolean isEnabled() {
        return true;
    }
}
