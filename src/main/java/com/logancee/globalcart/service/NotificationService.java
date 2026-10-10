package com.logancee.globalcart.service;

import com.logancee.globalcart.model.NotificationItem;
import com.logancee.globalcart.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final SpannerDataService spannerDataService;

    public NotificationService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public List<NotificationItem> getUserNotifications(String userEmail) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return spannerDataService.getUserNotifications(user.getId());
    }

    public void markAsRead(String userEmail, String notificationId) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        List<NotificationItem> list = spannerDataService.getUserNotifications(user.getId());
        list.stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .ifPresent(n -> n.setRead(true));
    }
}
