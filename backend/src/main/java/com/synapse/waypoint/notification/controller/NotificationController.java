package com.synapse.waypoint.notification.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.notification.dto.NotificationListResponse;
import com.synapse.waypoint.notification.dto.NotificationResponse;
import com.synapse.waypoint.notification.dto.ReadAllResponse;
import com.synapse.waypoint.notification.service.NotificationInboxService;

/** The signed-in user's own notifications — docs/api.md §5. */
@RestController
@RequestMapping("/api/notifications")
class NotificationController {

    private final NotificationInboxService inbox;
    private final CurrentUser currentUser;

    NotificationController(NotificationInboxService inbox, CurrentUser currentUser) {
        this.inbox = inbox;
        this.currentUser = currentUser;
    }

    @GetMapping
    NotificationListResponse list(@RequestParam(defaultValue = "false") boolean unread) {
        return inbox.list(currentUser.id(), unread);
    }

    @PostMapping("/{id}/read")
    NotificationResponse markRead(@PathVariable String id) {
        return inbox.markRead(currentUser.id(), id);
    }

    @PostMapping("/read-all")
    ReadAllResponse markAllRead() {
        return inbox.markAllRead(currentUser.id());
    }
}
