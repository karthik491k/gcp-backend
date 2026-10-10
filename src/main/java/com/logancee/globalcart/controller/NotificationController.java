package com.logancee.globalcart.controller;

import com.logancee.globalcart.model.NotificationItem;
import com.logancee.globalcart.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationItem>> getNotifications(Authentication auth) {
        return ResponseEntity.ok(notificationService.getUserNotifications(auth.getName()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(Authentication auth, @PathVariable String id) {
        notificationService.markAsRead(auth.getName(), id);
        return ResponseEntity.ok().build();
    }
}
