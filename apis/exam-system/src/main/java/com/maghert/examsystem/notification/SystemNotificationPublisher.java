package com.maghert.examsystem.notification;

public interface SystemNotificationPublisher {

    void publish(SystemNotificationEvent event);
}
